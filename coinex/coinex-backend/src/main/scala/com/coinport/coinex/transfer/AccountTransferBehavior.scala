package com.coinport.coinex.transfer

import akka.actor.Actor._
import akka.event.LoggingAdapter
import com.coinport.coinex.data._
import com.coinport.coinex.common.mongo.SimpleJsonMongoCollection
import com.coinport.coinex.serializers.ThriftEnumJson4sSerialization
import com.mongodb.casbah.Imports._
import org.json4s._
import scala.collection.mutable.{ ListBuffer, Map }

import TransferType._

trait AccountTransferBehavior {

  val db: MongoDB
  implicit val manager: AccountTransferManager
  implicit val logger: LoggingAdapter
  var transferDebug: Boolean = false
  var confirmableHeight = collection.Map.empty[Currency, Int]
  var succeededRetainHeight = collection.Map.empty[Currency, Int]
  val transferHandlerObjectMap = Map.empty[TransferType, CryptoCurrencyTransferBase]

  def setTransferConfig(transferConfig: AccountTransferConfig) = {
    transferDebug = transferConfig.transferDebug
    confirmableHeight ++= transferConfig.confirmableHeight
    succeededRetainHeight ++= transferConfig.succeededRetainHeight
  }

  def isCryptoCurrency(currency: Currency): Boolean = {
    currency.value >= Currency.Btc.value
  }

  def intTransferHandlerObjectMap() {
    val env = new TransferEnv(manager, transferHandler, transferItemHandler, logger, confirmableHeight, succeededRetainHeight)
    transferHandlerObjectMap += Deposit -> CryptoCurrencyTransferDepositHandler.setEnv(env)
    transferHandlerObjectMap += UserToHot -> CryptoCurrencyTransferUserToHotHandler.setEnv(env)
    transferHandlerObjectMap += Withdrawal -> CryptoCurrencyTransferWithdrawalHandler.setEnv(env)
    transferHandlerObjectMap += HotToCold -> CryptoCurrencyTransferHotToColdHandler.setEnv(env)
    transferHandlerObjectMap += ColdToHot -> CryptoCurrencyTransferColdToHotHandler.setEnv(env)
    manager.setTransferHandlers(transferHandlerObjectMap)
    CryptoCurrencyTransferUnknownHandler.setEnv(env)
  }

  def updateState: Receive = {

    case DoRequestTransfer(t) =>
      if (isCryptoCurrency(t.currency) && !transferDebug) {
        t.`type` match {
          case TransferType.Deposit => //Do nothing
          case TransferType.UserToHot =>
          case TransferType.Withdrawal =>
            transferHandler.put(t)
          case TransferType.ColdToHot => //Just log, will confirmed by admin
            transferHandler.put(t)
          case TransferType.HotToCold =>
            transferHandler.put(t)
            val from = CryptoCurrencyTransactionPort("", None, Some(t.amount), Some(t.userId))
            val to = CryptoCurrencyTransactionPort("", None, Some(t.amount), Some(t.userId))
            prepareBitwayMsg(t, Some(from), Some(to), transferHandlerObjectMap(HotToCold).asInstanceOf[CryptoCurrencyTransferWithdrawalLikeBase])
          case TransferType.Unknown =>
            transferHandler.put(t)
        }
      } else {
        transferHandler.put(t)
      }
      manager.setLastTransferId(t.id)

    case AdminConfirmTransferFailure(t, _) => transferHandler.put(t)

    case DoCancelTransfer(t) => transferHandler.put(t)

    case AdminConfirmTransferSuccess(t) => {
      if (isCryptoCurrency(t.currency) && !transferDebug) {
        t.`type` match {
          case TransferType.Withdrawal =>
            val transferAmount = t.fee match {
              case Some(withdrawalFee: Fee) if withdrawalFee.amount > 0 => t.amount - withdrawalFee.amount
              case _ => t.amount
            }
            val to = CryptoCurrencyTransactionPort(t.address.get, None, Some(transferAmount), Some(t.userId))
            prepareBitwayMsg(t, None, Some(to), transferHandlerObjectMap(Withdrawal).asInstanceOf[CryptoCurrencyTransferWithdrawalLikeBase])
          case _ => // Just handle other type, do nothing
        }
      }
      transferHandler.put(t)
    }

    case m @ MultiCryptoCurrencyTransactionMessage(currency, txs, newIndex: Option[BlockIndex]) =>
      logger.info(s">>>>>>>>>>>>>>>>>>>>> updateState  => ${m.toString}")
      if (manager.getLastBlockHeight(currency) > 0) newIndex foreach {
        reOrgBlockIndex =>
          transferHandlerObjectMap.values foreach {
            _.reOrganize(currency, reOrgBlockIndex, manager)
          }
      }

      transferHandlerObjectMap.values foreach { _.init() }

      txs foreach {
        tx =>
          tx.txType match {
            case None =>
              logger.warning(s"Unexpected tx meet : ${tx.toString}")
              CryptoCurrencyTransferUnknownHandler.handleTx(currency, tx)
            case Some(txType) =>
              transferHandlerObjectMap.contains(txType) match {
                case true =>
                  transferHandlerObjectMap(txType).handleTx(currency, tx)
                case _ =>
                  logger.warning(s"Unknown tx meet : ${tx.toString}")
              }
          }
      }
      transferHandlerObjectMap.values foreach { _.checkConfirm(currency) }

    case rs @ TransferCryptoCurrencyResult(currency, _, request) =>
      logger.info(s">>>>>>>>>>>>>>>>>>>>> updateState  => ${rs.toString}")
      transferHandlerObjectMap.values foreach { _.init() }
      request.get.transferInfos foreach {
        info =>
          transferHandlerObjectMap(request.get.`type`).handleBitwayFail(info, currency)
      }

    case mr @ MultiTransferCryptoCurrencyResult(currency, _, transferInfos) =>
      logger.info(s">>>>>>>>>>>>>>>>>>>>> updateState  => ${mr.toString}")
      transferHandlerObjectMap.values foreach { _.init() }
      transferInfos.get.keys foreach {
        txType =>
          transferInfos.get.get(txType).get foreach {
            info =>
              transferHandlerObjectMap(txType).handleBitwayFail(info, currency)
          }
      }
  }

  def prepareBitwayMsg(transfer: AccountTransfer, from: Option[CryptoCurrencyTransactionPort],
    to: Option[CryptoCurrencyTransactionPort], handler: CryptoCurrencyTransferWithdrawalLikeBase) {
    handler.init()
    handler.newHandlerFromAccountTransfer(transfer, from, to)
  }

  def batchBitwayMessage(currency: Currency): Map[TransferType, List[CryptoCurrencyTransferInfo]] = {
    val multiCryptoCurrencyTransfers = Map.empty[TransferType, List[CryptoCurrencyTransferInfo]]
    transferHandlerObjectMap.keys map {
      key =>
        val infos = transferHandlerObjectMap(key).getMsgToBitway(currency)
        if (!infos.isEmpty)
          multiCryptoCurrencyTransfers.put(key, infos)
    }
    multiCryptoCurrencyTransfers
  }

  def batchAccountMessage(currency: Currency): Map[String, AccountTransfersWithMinerFee] = {
    val multiAccountTransfers = Map.empty[String, AccountTransfersWithMinerFee]
    transferHandlerObjectMap.keys map {
      key =>
        val sigId2AccountTransferMap: Map[String, (ListBuffer[AccountTransfer], Option[Long])] = transferHandlerObjectMap(key).getMsgToAccount(currency)
        if (!sigId2AccountTransferMap.isEmpty) {
          sigId2AccountTransferMap.keys map {
            sigId =>
              val tansfersWithMinerFee = sigId2AccountTransferMap(sigId)
              if (!tansfersWithMinerFee._1.isEmpty) {
                multiAccountTransfers.put(sigId, AccountTransfersWithMinerFee(tansfersWithMinerFee._1.toList, tansfersWithMinerFee._2))
              }
          }
        }
    }
    multiAccountTransfers
  }

  implicit val transferHandler = new SimpleJsonMongoCollection[AccountTransfer, AccountTransfer.Immutable]() {
    lazy val coll = db("transfers")
    override implicit val formats: Formats = ThriftEnumJson4sSerialization.formats + new FeeSerializer
    def extractId(item: AccountTransfer) = item.id

    def getQueryDBObject(q: QueryTransfer): MongoDBObject = {
      var query = MongoDBObject()
      if (q.uid.isDefined) query ++= MongoDBObject(DATA + "." + AccountTransfer.UserIdField.name -> q.uid.get)
      if (q.currency.isDefined) query ++= MongoDBObject(DATA + "." + AccountTransfer.CurrencyField.name -> q.currency.get.name)
      if (q.types.nonEmpty) query ++= $or(q.types.map(t => DATA + "." + AccountTransfer.TypeField.name -> t.toString): _*)
      if (q.status.isDefined) query ++= MongoDBObject(DATA + "." + AccountTransfer.StatusField.name -> q.status.get.name)
      if (q.spanCur.isDefined) query ++= (DATA + "." + AccountTransfer.CreatedField.name $lte q.spanCur.get.from $gte q.spanCur.get.to)
      query
    }
  }

  implicit val transferItemHandler = new SimpleJsonMongoCollection[CryptoCurrencyTransferItem, CryptoCurrencyTransferItem.Immutable]() {
    lazy val coll = db("transferitems")
    def extractId(item: CryptoCurrencyTransferItem) = item.id

    def getQueryDBObject(q: QueryCryptoCurrencyTransfer): MongoDBObject = {
      var query = MongoDBObject()
      if (q.id.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.IdField.name -> q.id.get)
      if (q.sigId.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.SigIdField.name -> q.sigId.get)
      if (q.txid.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.TxidField.name -> q.txid.get.toString)
      if (q.currency.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.CurrencyField.name -> q.currency.get.name)
      if (q.txType.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.TxTypeField.name -> q.txType.get.name)
      if (q.status.isDefined) query ++= MongoDBObject(DATA + "." + CryptoCurrencyTransferItem.StatusField.name -> q.status.get.name)
      if (q.spanCur.isDefined) query ++= (DATA + "." + CryptoCurrencyTransferItem.CreatedField.name $lte q.spanCur.get.from $gte q.spanCur.get.to)
      query
    }
  }
}

class FeeSerializer(implicit man: Manifest[Fee.Immutable]) extends CustomSerializer[Fee](format => ({
  case obj: JValue => Extraction.extract(obj)(ThriftEnumJson4sSerialization.formats, man)
}, {
  case x: Fee => Extraction.decompose(x)(ThriftEnumJson4sSerialization.formats)
}))

class TransferEnv(val manager: AccountTransferManager,
  val transferHandler: SimpleJsonMongoCollection[AccountTransfer, AccountTransfer.Immutable],
  val transferItemHandler: SimpleJsonMongoCollection[CryptoCurrencyTransferItem, CryptoCurrencyTransferItem.Immutable],
  val logger: LoggingAdapter,
  val confirmableHeight: collection.immutable.Map[Currency, Int],
  val succeededRetainHeight: collection.immutable.Map[Currency, Int])

