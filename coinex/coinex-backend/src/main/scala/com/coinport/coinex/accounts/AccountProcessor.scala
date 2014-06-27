/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.accounts

import akka.actor._
import akka.actor.Actor.Receive
import akka.event.{ LoggingAdapter, LoggingReceive }
import akka.persistence.SnapshotOffer
import akka.persistence._

import com.coinport.coinex.common._
import com.coinport.coinex.common.Constants._
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.common.ExtendedProcessor
import com.coinport.coinex.common.support._
import com.coinport.coinex.data._
import com.coinport.coinex.fee._
import ErrorCode._
import Implicits._
import TransferType._
import Currency._

class AccountProcessor(
  marketProcessors: Map[MarketSide, ActorRef],
  marketUpdateProcessoressorPath: ActorPath,
  depositWithdrawProcessorPath: ActorPath,
  accountConfig: AccountConfig) extends ExtendedProcessor with EventsourcedProcessor
    with AccountManagerBehavior with ActorLogging {

  val feeConfig = accountConfig.feeConfig
  override implicit val logger = log

  private val MAX_PRICE = 1E8.toDouble // 100000000.00000001 can be preserved by toDouble.
  private var hotColdTransferLastTransferTime = Map.empty[Currency, Long]

  override val processorId = ACCOUNT_PROCESSOR <<
  val channelToMarketProcessors = createChannelTo(MARKET_PROCESSOR <<) // DO NOT CHANGE
  val channelToMarketUpdateProcessor = createChannelTo(MARKET_UPDATE_PROCESSOR<<) // DO NOT CHANGE
  val channelToDepositWithdrawalProcessor = createChannelTo(ACCOUNT_TRANSFER_PROCESSOR<<) // DO NOT CHANGE
  val manager = new AccountManager(1E12.toLong, accountConfig.hotColdTransfer)

  override def identifyChannel: PartialFunction[Any, String] = {
    case as: AdminConfirmTransferSuccess => "tsf"
    case af: AdminConfirmTransferFailure => "tsf"
    case rt: RequestTransferFailed => "tsf"
    case cs: CryptoTransferSucceeded => "tsf"
    case cf: CryptoTransferFailed => "tsf"
    case cl: DoCancelTransfer => "tsf"
    case OrderSubmitted(originOrderInfo, txs) => "mp_" + originOrderInfo.side.s
    case OrderCancelled(side, order) => "mp_" + side.s
  }

  def receiveRecover = PartialFunction.empty[Any, Unit]

  def receiveCommand = LoggingReceive {
    case DoRequestTransfer(t) => t.`type` match {
      case Withdrawal =>
        val adjustment = CashAccount(t.currency, -t.amount, 0, t.amount)
        if (!manager.canUpdateCashAccount(t.userId, adjustment)) {
          sender ! RequestTransferFailed(InsufficientFund)
        } else if (!manager.canUpdateHotAccount(adjustment)) {
          sender ! RequestTransferFailed(InsufficientHot)
        } else {
          val updated = countFee(t.copy(created = Some(System.currentTimeMillis)))
          persist(DoRequestTransfer(updated)) { event =>
            updateState(event)
            channelToDepositWithdrawalProcessor forward Deliver(Persistent(event), depositWithdrawProcessorPath)
          }
        }

      case Deposit =>
        if (t.amount <= 0) {
          sender ! RequestTransferFailed(InvalidAmount)
        } else {
          val updated = countFee(t.copy(created = Some(System.currentTimeMillis)))
          persist(DoRequestTransfer(updated)) { event =>
            updateState(event)
            channelToDepositWithdrawalProcessor forward Deliver(Persistent(event), depositWithdrawProcessorPath)
          }
        }

      case HotToCold =>
        if (!manager.canUpdateHotAccount(CashAccount(t.currency, -t.amount, 0, t.amount))) {
          sender ! RequestTransferFailed(InsufficientHot)
        } else {
          persist(DoRequestTransfer(t)) { event =>
            updateState(event)
            channelToDepositWithdrawalProcessor forward Deliver(Persistent(event), depositWithdrawProcessorPath)
          }
        }

      case ColdToHot =>
        if (!manager.canUpdateColdAccount(CashAccount(t.currency, -t.amount, 0, t.amount))) {
          sender ! RequestTransferFailed(InsufficientCold)
        } else {
          persist(DoRequestTransfer(t)) { event =>
            updateState(event)
            channelToDepositWithdrawalProcessor forward Deliver(Persistent(event), depositWithdrawProcessorPath)
          }
        }
      case _ => // frontend can't send UserToHot
    }

    case p @ ConfirmablePersistent(m: AdminConfirmTransferSuccess, _, _) =>
      persist(m.copy(transfer = appendFeeIfNecessary(m.transfer))) { event =>
        confirm(p)
        updateState(event)
      }

    case p @ ConfirmablePersistent(m: CryptoTransferSucceeded, _, _) =>
      persist(m.copy(transfers = m.transfers.map(appendFeeIfNecessary(_)))) {
        event =>
          confirm(p)
          updateState(event)
          val currency = m.transfers(0).currency
          if (accountConfig.enableHotColdTransfer && (m.txType == Withdrawal || m.txType == UserToHot) &&
            System.currentTimeMillis - hotColdTransferLastTransferTime.getOrElse(currency, 0L) > accountConfig.hotColdTransferInterval) {
            hotColdTransferLastTransferTime += (currency -> System.currentTimeMillis)
            transferHotColdIfNeed(currency)
          }
      }

    case m: CryptoTransferResult =>
      hanleCryptoTransferResult(m)

    case p @ ConfirmablePersistent(m: CryptoTransferResult, _, _) =>
      confirm(p)
      hanleCryptoTransferResult(m)

    case DoRequestGenerateABCode(userId, amount, _, _) => {
      val adjustment = CashAccount(Currency.Cny, -amount, amount, 0)
      if (!manager.canUpdateCashAccount(userId, adjustment)) {
        sender ! RequestGenerateABCodeFailed(InsufficientFund)
      } else {
        val (a, b) = manager.generateABCode()
        persist(DoRequestGenerateABCode(userId, amount, Some(a), Some(b))) { event =>
          updateState(event)
          sender ! RequestGenerateABCodeSucceeded(a, b)
        }
      }
    }

    case DoRequestACodeQuery(userId, codeA) => {
      if (!manager.isCodeAAvailable(userId, codeA)) {
        sender ! RequestACodeQueryFailed(LockedACode)
      } else {
        persist(DoRequestACodeQuery(userId, codeA)) { event =>
          updateState(event)
          sender ! RequestACodeQuerySucceeded(codeA, RechargeCodeStatus.Frozen,
            manager.abCodeMap(manager.codeAIndexMap(codeA)).amount)
        }
      }
    }

    case DoRequestBCodeRecharge(userId, codeB) => {
      val (canRecharge, error) = manager.isCodeBAvailable(userId, codeB)
      canRecharge match {
        case false => sender ! RequestBCodeRechargeFailed(error.asInstanceOf[ErrorCode])
        case true => {
          persist(DoRequestBCodeRecharge(userId, codeB)) { event =>
            updateState(event)
            sender ! RequestBCodeRechargeSucceeded(codeB, RechargeCodeStatus.Confirming,
              manager.abCodeMap(manager.codeBIndexMap(codeB)).amount)
          }
        }
      }
    }

    case DoRequestConfirmRC(userId, codeB, amount) => {
      val (canRecharge, error) = manager.verifyConfirm(userId, codeB)
      canRecharge match {
        case false => sender ! RequestConfirmRCFailed(error.asInstanceOf[ErrorCode])
        case true => {
          persist(DoRequestConfirmRC(userId, codeB, amount)) { event =>
            updateState(event)
            sender ! RequestConfirmRCSucceeded(codeB, RechargeCodeStatus.RechargeDone,
              manager.abCodeMap(manager.codeBIndexMap(codeB)).amount)
          }
        }
      }
    }

    case p @ ConfirmablePersistent(m: AdminConfirmTransferFailure, _, _) =>
      persist(m) { event => confirm(p); updateState(event) }

    case p @ ConfirmablePersistent(m: CryptoTransferFailed, _, _) =>
      persist(m) { event => confirm(p); updateState(event) }

    case p @ ConfirmablePersistent(m: RequestTransferFailed, _, _) =>
      log.error(s"Failed by AccountTransferProcessor for reason: ${m.error.toString}")

    case p @ ConfirmablePersistent(m: DoCancelTransfer, _, _) =>
      persist(m) { event => confirm(p); updateState(event) }

    case DoSubmitOrder(side, order) =>
      if (order.quantity <= 0) {
        sender ! SubmitOrderFailed(side, order, ErrorCode.InvalidAmount)
      } else {
        val adjustment = CashAccount(side.outCurrency, -order.quantity, order.quantity, 0)
        if (!manager.canUpdateCashAccount(order.userId, adjustment)) {
          sender ! SubmitOrderFailed(side, order, ErrorCode.InsufficientFund)
        } else {
          val updated = order.copy(
            id = manager.getOrderId,
            timestamp = Some(System.currentTimeMillis))

          if (updated.price.isDefined && (updated.price.get == 0.0 || updated.price.get > MAX_PRICE)) {
            sender ! SubmitOrderFailed(side, order, ErrorCode.PriceOutOfRange)
          } else {
            persist(DoSubmitOrder(side, updated)) { event =>
              channelToMarketProcessors forward Deliver(Persistent(OrderFundFrozen(side, updated)), getProcessorPath(side))
              updateState(event)
            }
          }
        }
      }

    case p @ ConfirmablePersistent(event: OrderSubmitted, seq, _) =>
      persist(countFee(event)) { event =>
        confirm(p)
        sender ! event
        updateState(event)
        channelToMarketUpdateProcessor forward Deliver(Persistent(event), marketUpdateProcessoressorPath)
      }

    case p @ ConfirmablePersistent(event: OrderCancelled, seq, _) =>
      persist(countFee(event)) { event =>
        confirm(p)
        sender ! event
        updateState(event)
        channelToMarketUpdateProcessor forward Deliver(Persistent(event), marketUpdateProcessoressorPath)
      }
  }

  private def transferHotColdIfNeed(currency: Currency) {
    manager.needHotColdTransfer(currency) match {
      case None =>
      case Some(amount) if amount == 0 =>
      case Some(amount) if amount > 0 =>
        self ! DoRequestTransfer(AccountTransfer(0, 0, HotToCold, currency, amount, created = Some(System.currentTimeMillis)))
      case Some(amount) if amount < 0 =>
        self ! DoRequestTransfer(AccountTransfer(0, 0, ColdToHot, currency, -amount, created = Some(System.currentTimeMillis)))
    }
  }

  private def getProcessorPath(side: MarketSide): ActorPath = {
    marketProcessors.getOrElse(side, marketProcessors(side.reverse)).path
  }

  private def appendFeeIfNecessary(t: AccountTransfer) = {
    if ((t.`type` == Withdrawal || t.`type` == Deposit) && !t.fee.isDefined && t.status == TransferStatus.Succeeded) {
      countFee(t)
    } else {
      t
    }
  }

  private def hanleCryptoTransferResult(m: CryptoTransferResult) {
    persist(m.copy(multiTransfers = m.multiTransfers.map(kv => kv._1 -> kv._2.copy(transfers = kv._2.transfers map { appendFeeIfNecessary(_) })))) {
      event =>
        updateState(event)
        val currency = m.multiTransfers.values.head.transfers(0).currency
        var needCheckHotColdTransfer = false
        m.multiTransfers.values.foreach {
          transferWithMinerFee =>
            transferWithMinerFee.transfers.foreach {
              _.`type` match {
                case Withdrawal => needCheckHotColdTransfer = true
                case UserToHot => needCheckHotColdTransfer = true
                case _ =>
              }
            }
        }
        if (accountConfig.enableHotColdTransfer && needCheckHotColdTransfer &&
          System.currentTimeMillis - hotColdTransferLastTransferTime.getOrElse(currency, 0L) > accountConfig.hotColdTransferInterval) {
          hotColdTransferLastTransferTime += (currency -> System.currentTimeMillis)
          transferHotColdIfNeed(currency)
        }
    }
  }
}

trait AccountManagerBehavior extends CountFeeSupport {
  val manager: AccountManager
  implicit val logger: LoggingAdapter

  def updateState: Receive = {

    case DoRequestGenerateABCode(userId, amount, Some(a), Some(b)) =>
      manager.createABCodeTransaction(userId, a, b, amount)
      manager.updateCashAccount(userId, CashAccount(Currency.Cny, -amount, amount, 0))
    case DoRequestACodeQuery(userId, codeA) => manager.freezeABCode(userId, codeA)
    case DoRequestBCodeRecharge(userId, codeB) => manager.bCodeRecharge(userId, codeB)
    case DoRequestConfirmRC(userId, codeB, amount) => {
      manager.confirmRecharge(userId, codeB)
      manager.updateCashAccount(userId, CashAccount(Currency.Cny, 0, -amount, 0))
      manager.updateCashAccount(manager.abCodeMap(manager.codeBIndexMap(codeB)).dUserId.get,
        CashAccount(Currency.Cny, amount, 0, 0))
    }

    case DoRequestTransfer(t) =>
      t.`type` match {
        case Withdrawal =>
          manager.updateCashAccount(t.userId, CashAccount(t.currency, -t.amount, 0, t.amount))
          manager.updateHotCashAccount(CashAccount(t.currency, -t.amount, 0, t.amount))
        case HotToCold =>
          manager.updateHotCashAccount(CashAccount(t.currency, -t.amount, 0, t.amount))
        case _ =>
      }

    case AdminConfirmTransferSuccess(t) =>
      succeededTransfer(t)

    case AdminConfirmTransferFailure(t, _) =>
      failedTransfer(t)

    case CryptoTransferSucceeded(_, t, minerFee) => {
      t foreach { succeededTransfer(_) }
      minerFee foreach { substractMinerFee(t(0).currency, _) }
    }

    case CryptoTransferResult(multiTransfers) => {
      //      println(s">>>>>>>>>>>>>>>>>>>>> AccountProcessor got success accountTransfer => ${t.toString}")
      multiTransfers.values foreach {
        transferWithFee =>
          transferWithFee.transfers.foreach {
            transfer =>
              transfer.status match {
                case TransferStatus.Succeeded => succeededTransfer(transfer)
                case TransferStatus.Failed => failedTransfer(transfer)
                case _ => logger.error("Unexpected transferStatus" + transfer.toString)
              }
          }
          transferWithFee.minerFee foreach (substractMinerFee(transferWithFee.transfers(0).currency, _))
      }
    }

    case CryptoTransferFailed(t, _) => {
      failedTransfer(t)
    }

    case DoCancelTransfer(t) =>
      failedTransfer(t)

    case DoSubmitOrder(side: MarketSide, order) =>
      manager.updateCashAccount(order.userId, CashAccount(side.outCurrency, -order.quantity, order.quantity, 0))
      manager.setLastOrderId(order.id)

    case OrderSubmitted(originOrderInfo, txs) =>
      val side = originOrderInfo.side
      txs foreach { tx =>
        val (takerUpdate, makerUpdate, fees) = (tx.takerUpdate, tx.makerUpdate, tx.fees)
        manager.transferFundFromLocked(takerUpdate.userId, makerUpdate.userId, side.outCurrency, takerUpdate.outAmount)
        manager.transferFundFromLocked(makerUpdate.userId, takerUpdate.userId, side.inCurrency, makerUpdate.outAmount)
        refund(side.inCurrency, makerUpdate.current)

        tx.fees.getOrElse(Nil) foreach { f =>
          manager.transferFundFromAvailable(f.payer, f.payee.getOrElse(COINPORT_UID), f.currency, f.amount)
        }

      }
      val order = txs.lastOption.map(_.takerUpdate.current).getOrElse(originOrderInfo.order)
      refund(side.outCurrency, order)

    case OrderCancelled(side, order) =>
      manager.conditionalRefund(true)(side.outCurrency, order)
  }

  private def succeededTransfer(t: AccountTransfer) {
    t.`type` match {
      case Deposit =>
        manager.updateCashAccount(t.userId, CashAccount(t.currency, t.amount, 0, 0))
        t.fee match {
          case Some(f) if (f.amount > 0) =>
            manager.transferFundFromAvailable(f.payer, f.payee.getOrElse(COINPORT_UID), f.currency, f.amount)
          case _ => None
        }
      case Withdrawal =>
        t.fee match {
          case Some(f) if (f.amount > 0) =>
            manager.transferFundFromPendingWithdrawal(f.payer, f.payee.getOrElse(COINPORT_UID), f.currency, f.amount)
            manager.updateCashAccount(t.userId, CashAccount(t.currency, 0, 0, f.amount - t.amount))
            manager.updateHotCashAccount(CashAccount(t.currency, f.amount, 0, -t.amount))
          case _ =>
            manager.updateCashAccount(t.userId, CashAccount(t.currency, 0, 0, -t.amount))
            manager.updateHotCashAccount(CashAccount(t.currency, 0, 0, -t.amount))
        }
      case UserToHot =>
        manager.updateHotCashAccount(CashAccount(t.currency, t.amount, 0, 0))
      case HotToCold =>
        manager.updateHotCashAccount(CashAccount(t.currency, 0, 0, -t.amount))
        manager.updateColdCashAccount(CashAccount(t.currency, t.amount, 0, 0))
      case ColdToHot =>
        manager.updateColdCashAccount(CashAccount(t.currency, -t.amount, 0, 0))
        manager.updateHotCashAccount(CashAccount(t.currency, t.amount, 0, 0))
      case TransferType.Unknown =>
    }
  }

  private def substractMinerFee(currency: Currency, minerFee: Long) {
    manager.updateHotCashAccount(CashAccount(currency, -minerFee, 0, 0))
    manager.updateCoinportAccount(CashAccount(currency, -minerFee, 0, 0))
  }

  private def failedTransfer(t: AccountTransfer) {
    t.`type` match {
      case Withdrawal =>
        manager.updateCashAccount(t.userId, CashAccount(t.currency, t.amount, 0, -t.amount))
        manager.updateHotCashAccount(CashAccount(t.currency, t.amount, 0, -t.amount))
      case HotToCold => manager.updateHotCashAccount(CashAccount(t.currency, t.amount, 0, -t.amount))
      case _ =>
    }
  }

  private def refund(currency: Currency, order: Order) = order.refund match {
    case Some(Refund(_, quantity)) if quantity > 0 =>
      manager.refund(order.userId, currency, quantity)
    case _ =>
  }
}

