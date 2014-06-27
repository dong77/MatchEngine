package com.coinport.coinex.transfer

import akka.actor._
import akka.event.LoggingReceive
import akka.event.LoggingAdapter
import akka.persistence._
import com.coinport.coinex.common.{ ExtendedProcessor, Manager }
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.common.support.ChannelSupport
import com.coinport.coinex.data._
import com.mongodb.casbah.Imports._
import com.twitter.util.Eval
import java.io.InputStream
import org.apache.commons.io.IOUtils
import scala.collection.mutable.Map

import ErrorCode._
import Implicits._
import TransferStatus._
import TransferType._

class AccountTransferProcessor(val db: MongoDB, accountProcessorPath: ActorPath, bitwayProcessors: collection.immutable.Map[Currency, ActorRef]) extends ExtendedProcessor
    with EventsourcedProcessor with ChannelSupport with AccountTransferBehavior with ActorLogging {
  override val processorId = ACCOUNT_TRANSFER_PROCESSOR <<

  lazy implicit val logger: LoggingAdapter = log

  implicit val manager = new AccountTransferManager()
  val accountTransferConfig = loadConfig(context.system.settings.config.getString("akka.exchange.transfer-path"))
  val transferDebugConfig = accountTransferConfig.transferDebug
  private val channelToAccountProcessor = createChannelTo(ACCOUNT_PROCESSOR <<) // DO NOT CHANGE
  private val bitwayChannels = bitwayProcessors.map(kv => kv._1 -> createChannelTo(BITWAY_PROCESSOR << kv._1))

  setTransferConfig(accountTransferConfig)
  intTransferHandlerObjectMap()

  private def loadConfig(configPath: String): AccountTransferConfig = {
    val in: InputStream = this.getClass.getClassLoader.getResourceAsStream(configPath)
    (new Eval()(IOUtils.toString(in))).asInstanceOf[AccountTransferConfig]
  }

  override def identifyChannel: PartialFunction[Any, String] = {
    case r: DoRequestTransfer => "account"
    case MultiCryptoCurrencyTransactionMessage(currency, _, _) => "bitway_" + currency.toString.toLowerCase()
    case TransferCryptoCurrencyResult(currency, _, _) => "bitway_" + currency.toString.toLowerCase()
    case MultiTransferCryptoCurrencyResult(currency, _, _) => "bitway_" + currency.toString.toLowerCase()
  }

  def receiveRecover = PartialFunction.empty[Any, Unit]

  def receiveCommand = LoggingReceive {
    case p @ ConfirmablePersistent(DoRequestTransfer(w), _, _) =>
      persist(DoRequestTransfer(w.copy(id = manager.getTransferId))) {
        event =>
          confirm(p)
          updateState(event)
          if (isCryptoCurrency(w.currency) && !transferDebugConfig) {
            w.`type` match {
              case TransferType.Deposit =>
                sender ! RequestTransferFailed(UnsupportTransferType)
              case TransferType.UserToHot =>
                sender ! RequestTransferFailed(UnsupportTransferType)
              case TransferType.Withdrawal => // accept wait for admin accept
              case TransferType.ColdToHot => // accept, wait for admin confirm
              case TransferType.HotToCold => // accept, save request to map
              case TransferType.Unknown => // accept wait for admin accept
                sender ! RequestTransferFailed(UnsupportTransferType)
            }
            sendBitwayMsg(w.currency)
          } else { // No need to send message, as accountProcessor will ignore it, just for integration test
            sender ! RequestTransferSucceeded(event.transfer) // wait for admin confirm
          }
      }

    case AdminConfirmTransferFailure(t, error) =>
      transferHandler.get(t.id) match {
        case Some(transfer) if transfer.status == Pending =>
          val updated = transfer.copy(updated = Some(System.currentTimeMillis), status = Failed, reason = Some(error))
          persist(AdminConfirmTransferFailure(updated, error)) {
            event =>
              sender ! AdminCommandResult(Ok)
              deliverToAccountManager(event)
              updateState(event)
          }
        case Some(_) => sender ! AdminCommandResult(AlreadyConfirmed)
        case None => sender ! AdminCommandResult(TransferNotExist)
      }

    case DoCancelTransfer(t) =>
      transferHandler.get(t.id) match {
        case Some(transfer) if transfer.status == Pending =>
          if (t.userId == transfer.userId) {
            val updated = transfer.copy(updated = Some(System.currentTimeMillis), status = Failed, reason = Some(ErrorCode.UserCanceled))
            if (transfer.`type` == ColdToHot || transfer.`type` == Withdrawal) {
              persist(DoCancelTransfer(updated)) {
                event =>
                  sender ! AdminCommandResult(Ok)
                  deliverToAccountManager(event)
                  updateState(event)
              }
            } else {
              sender ! AdminCommandResult(UnsupportTransferType)
            }
          } else {
            sender ! AdminCommandResult(UserAuthenFail)
          }
        case Some(_) => sender ! AdminCommandResult(AlreadyConfirmed)
        case None => sender ! AdminCommandResult(TransferNotExist)
      }

    case AdminConfirmTransferSuccess(t) =>
      transferHandler.get(t.id) match {
        case Some(transfer) if transfer.status == Pending =>
          if (isCryptoCurrency(transfer.currency) && !transferDebugConfig) {
            transfer.`type` match {
              case TransferType.Deposit => sender ! RequestTransferFailed(UnsupportTransferType)
              case TransferType.Withdrawal if transfer.address.isDefined =>
                val updated = transfer.copy(updated = Some(System.currentTimeMillis), status = Accepted)
                persist(AdminConfirmTransferSuccess(updated)) {
                  event =>
                    updateState(event)
                    sendBitwayMsg(updated.currency)
                    sender ! AdminCommandResult(Ok)
                }
              case TransferType.ColdToHot =>
                val updated = transfer.copy(updated = Some(System.currentTimeMillis), status = Accepted)
                persist(AdminConfirmTransferSuccess(updated)) {
                  event =>
                    updateState(event)
                    sender ! AdminCommandResult(Ok)
                }
              case _ =>
                sender ! RequestTransferFailed(UnsupportTransferType)
            }
          } else {
            val updated = transfer.copy(updated = Some(System.currentTimeMillis), status = Succeeded)
            persist(AdminConfirmTransferSuccess(updated)) {
              event =>
                deliverToAccountManager(event)
                updateState(event)
                sender ! AdminCommandResult(Ok)
            }
          }
        case Some(_) => sender ! AdminCommandResult(AlreadyConfirmed)
        case None => sender ! AdminCommandResult(TransferNotExist)
      }

    case p @ ConfirmablePersistent(msg: MultiCryptoCurrencyTransactionMessage, _, _) =>
      persist(msg) {
        event =>
          confirm(p)
          updateState(event)
          sendBitwayMsg(event.currency)
          sendAccountMsg(event.currency)
      }

    case msg: MultiCryptoCurrencyTransactionMessage =>
      persist(msg) {
        event =>
          updateState(event)
          sendBitwayMsg(event.currency)
          sendAccountMsg(event.currency)
      }

    case tr: TransferCryptoCurrencyResult =>
      if (tr.error != ErrorCode.Ok && tr.request.isDefined && !tr.request.get.transferInfos.isEmpty) {
        persist(tr) {
          event =>
            updateState(event)
            sendBitwayMsg(event.currency)
            sendAccountMsg(event.currency)
        }
      }

    case mr: MultiTransferCryptoCurrencyResult =>
      if (mr.error != ErrorCode.Ok && mr.transferInfos.isDefined && !mr.transferInfos.get.isEmpty) {
        persist(mr) {
          event =>
            updateState(event)
            sendBitwayMsg(event.currency)
            sendAccountMsg(event.currency)
        }
      }
  }

  private def sendBitwayMsg(currency: Currency) {
    val transfersToBitway = batchBitwayMessage(currency)
    if (!transfersToBitway.isEmpty) {
      deliverToBitwayProcessor(currency, MultiTransferCryptoCurrency(currency, transfersToBitway))
    }
  }

  private def sendAccountMsg(currency: Currency) {
    val transfersToAccount: Map[String, AccountTransfersWithMinerFee] = batchAccountMessage(currency)
    if (!transfersToAccount.isEmpty) {
      deliverToAccountManager(CryptoTransferResult(transfersToAccount))
    }
  }

  private def deliverToAccountManager(event: Any) = {
    log.info(s">>>>>>>>>>>>>>>>>>>>> deliverToAccountManager => event = ${event.toString}")
    channelToAccountProcessor ! Deliver(Persistent(event), accountProcessorPath)
  }

  private def deliverToBitwayProcessor(currency: Currency, event: Any) = {
    log.info(s">>>>>>>>>>>>>>>>>>>>> deliverToBitwayProcessor => currency = ${currency.toString}, event = ${event.toString}, path = ${bitwayProcessors(currency).path.toString}")
    bitwayChannels(currency) ! Deliver(Persistent(event), bitwayProcessors(currency).path)
  }
}

class AccountTransferManager() extends Manager[TAccountTransferState] {
  private var lastTransferId = 1E12.toLong
  private var lastTransferItemId = 6E12.toLong
  private var lastBlockHeight = Map.empty[Currency, Long]
  private val transferMapInnner = Map.empty[TransferType, Map[Long, CryptoCurrencyTransferItem]]
  private val succeededMapInnner = Map.empty[TransferType, Map[Long, CryptoCurrencyTransferItem]]
  private val sigId2MinerFeeMapInnner = Map.empty[TransferType, Map[String, Long]]
  private var transferHandlerObjectMap = Map.empty[TransferType, CryptoCurrencyTransferBase]

  def setTransferHandlers(transferHandlerObjectMap: Map[TransferType, CryptoCurrencyTransferBase]) {
    this.transferHandlerObjectMap = transferHandlerObjectMap
  }

  def getSnapshot = {
    transferMapInnner.clear()
    succeededMapInnner.clear()
    sigId2MinerFeeMapInnner.clear()
    transferHandlerObjectMap.keys foreach {
      txType =>
        val handler = transferHandlerObjectMap(txType)
        transferMapInnner.put(txType, handler.getTransferItemsMap())
        succeededMapInnner.put(txType, handler.getSucceededItemsMap())
        sigId2MinerFeeMapInnner.put(txType, handler.getSigId2MinerFeeMap())
    }
    TAccountTransferState(
      lastTransferId,
      lastTransferItemId,
      lastBlockHeight,
      transferMapInnner,
      succeededMapInnner,
      sigId2MinerFeeMapInnner,
      getFiltersSnapshot)
  }

  def loadSnapshot(s: TAccountTransferState) = {
    lastTransferId = s.lastTransferId
    lastTransferItemId = s.lastTransferItemId
    lastBlockHeight ++= s.lastBlockHeight
    transferHandlerObjectMap.keys foreach {
      txType =>
        val handler = transferHandlerObjectMap(txType)
        handler.loadSnapshotItems(s.transferMap(txType))
        handler.loadSnapshotSucceededItems(s.succeededMap(txType))
        handler.loadSigId2MinerFeeMap(s.sigId2MinerFeeMapInnner(txType))
    }
    loadFiltersSnapshot(s.filters)
  }

  def getTransferId = lastTransferId + 1

  def setLastTransferId(id: Long) = { lastTransferId = id }

  def getLastTransferItemId = lastTransferItemId

  def getNewTransferItemId = {
    lastTransferItemId += 1
    lastTransferItemId
  }

  def getLastBlockHeight(currency: Currency): Long = lastBlockHeight.getOrElse(currency, 0L)

  def setLastBlockHeight(currency: Currency, height: Long) = { lastBlockHeight.put(currency, height) }
}
