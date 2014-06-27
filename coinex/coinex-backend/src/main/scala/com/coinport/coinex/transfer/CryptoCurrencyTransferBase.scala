package com.coinport.coinex.transfer

import akka.event.LoggingAdapter
import com.coinport.coinex.common.mongo.SimpleJsonMongoCollection
import com.coinport.coinex.data._
import com.coinport.coinex.data.TransferStatus._
import com.coinport.coinex.data.TransferType._
import com.coinport.coinex.common.Constants._
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

trait CryptoCurrencyTransferBase {
  val sigId2MinerFeeMap: Map[String, Long] = Map.empty[String, Long]
  val id2HandlerMap: Map[Long, CryptoCurrencyTransferHandler] = Map.empty[Long, CryptoCurrencyTransferHandler]
  val succeededId2HandlerMap: Map[Long, CryptoCurrencyTransferHandler] = Map.empty[Long, CryptoCurrencyTransferHandler]
  val msgBoxMap: Map[Long, CryptoCurrencyTransferItem] = Map.empty[Long, CryptoCurrencyTransferItem]

  var manager: AccountTransferManager = null
  var transferHandler: SimpleJsonMongoCollection[AccountTransfer, AccountTransfer.Immutable] = null
  var transferItemHandler: SimpleJsonMongoCollection[CryptoCurrencyTransferItem, CryptoCurrencyTransferItem.Immutable] = null
  var logger: LoggingAdapter = null
  var confirmableHeight = collection.immutable.Map.empty[Currency, Int]
  var succeededRetainHeight = collection.immutable.Map.empty[Currency, Int]

  def getNoneSigId = "NONE_SIGID_" + System.currentTimeMillis().toString

  implicit var env: TransferEnv = null

  def setEnv(env: TransferEnv): CryptoCurrencyTransferBase = {
    this.env = env
    manager = env.manager
    transferHandler = env.transferHandler
    transferItemHandler = env.transferItemHandler
    logger = env.logger
    confirmableHeight = env.confirmableHeight
    succeededRetainHeight = env.succeededRetainHeight
    this
  }

  def init(): CryptoCurrencyTransferBase = {
    msgBoxMap.clear()
    this
  }

  def handleTx(currency: Currency, tx: CryptoCurrencyTransaction) {
    refreshLastBlockHeight(currency, tx)
    innerHandleTx(currency, tx)
  }

  def handleBitwayFail(info: CryptoCurrencyTransferInfo, currency: Currency) {
    if (id2HandlerMap.contains(info.id)) {
      handleFailed(id2HandlerMap(info.id))
    } else {
      logger.warning(s"""${"~" * 50} bitway Fail not match existing item : id2HandleMap.size = ${id2HandlerMap.size}, info = ${info.toString}""")
    }
  }

  protected def innerHandleTx(currency: Currency, tx: CryptoCurrencyTransaction) {}

  def checkConfirm(currency: Currency) {
    val lastBlockHeight: Long = manager.getLastBlockHeight(currency)
    id2HandlerMap.values filter (_.item.currency == currency) foreach {
      handler =>
        if (handler.checkConfirm(lastBlockHeight) && handler.item.status.get == Succeeded) {
          handleSucceeded(handler.item.id)
        }
    }
    succeededId2HandlerMap.values filter (_.item.currency == currency) foreach {
      handler =>
        if (handler.checkRemoveSucceeded(lastBlockHeight)) {
          id2HandlerMap.remove(handler.item.id)
        }
    }
  }

  def loadSnapshotItems(snapshotItems: collection.Map[Long, CryptoCurrencyTransferItem]) {
    id2HandlerMap.clear()
    if (snapshotItems != null)
      snapshotItems.values map {
        item => id2HandlerMap.put(item.id, newHandlerFromItem(item))
      }
  }

  def loadSnapshotSucceededItems(snapshotItems: collection.Map[Long, CryptoCurrencyTransferItem]) {
    succeededId2HandlerMap.clear()
    if (snapshotItems != null)
      snapshotItems.values map {
        item => succeededId2HandlerMap.put(item.id, newHandlerFromItem(item))
      }
  }

  def loadSigId2MinerFeeMap(snapshotMap: collection.Map[String, Long]) {
    sigId2MinerFeeMap.clear()
    if (snapshotMap != null)
      sigId2MinerFeeMap ++= snapshotMap
  }

  def getTransferItemsMap(): Map[Long, CryptoCurrencyTransferItem] = {
    id2HandlerMap map {
      kv => kv._1 -> kv._2.item.copy()
    }
  }

  def getSucceededItemsMap(): Map[Long, CryptoCurrencyTransferItem] = {
    succeededId2HandlerMap map {
      kv => kv._1 -> kv._2.item.copy()
    }
  }

  def getSigId2MinerFeeMap(): Map[String, Long] = {
    sigId2MinerFeeMap.clone()
  }

  def reOrganize(currency: Currency, reOrgBlock: BlockIndex, manager: AccountTransferManager) {
    reOrgBlock.height match {
      case Some(reOrgHeight) if reOrgHeight < manager.getLastBlockHeight(currency) =>
        id2HandlerMap.values.filter(_.item.currency == currency) foreach {
          _.reOrgnize(reOrgHeight)
        }
        succeededId2HandlerMap.values filter (_.item.currency == currency) foreach {
          handler =>
            if (handler.reOrgnizeSucceeded(reOrgHeight)) {
              succeededId2HandlerMap.remove(handler.item.id)
            }
        }
      case _ =>
        logger.warning(s"""${"~" * 50} reOrgnize() wrong reOrgnize height : [${manager.getLastBlockHeight(currency)}, ${reOrgBlock.toString()}]""")
    }
  }

  def getMsgToBitway(currency: Currency): List[CryptoCurrencyTransferInfo] = {
    val resList = ListBuffer.empty[CryptoCurrencyTransferInfo]
    msgBoxMap.values map {
      item =>
        item2BitwayInfo(item) match {
          case Some(info) if item.currency == currency =>
            resList += info
          case _ =>
        }
    }
    resList.toList
  }

  // currency, (sigId, (list[AccountTransfer], minerFee))
  def getMsgToAccount(currency: Currency): Map[String, (ListBuffer[AccountTransfer], Option[Long])] = {
    val resMap = Map.empty[String, (ListBuffer[AccountTransfer], Option[Long])]
    val noneSigId = getNoneSigId
    msgBoxMap.values map {
      item =>
        item2AccountTransfer(item) match {
          case Some(info) if item.currency == currency =>
            val sigIdForKey = item.sigId match {
              //Failed item will not contain sigId
              case Some(sigId) => sigId
              case _ => noneSigId
            }
            if (!resMap.contains(sigIdForKey)) {
              val minerFee = item.status match {
                // only count Succeed transactions minerFee
                case Some(Succeeded) if item.sigId.isDefined => sigId2MinerFeeMap.remove(sigIdForKey)
                case Some(Succeeded) if !item.sigId.isDefined =>
                  logger.error(s"""${"~" * 50} getMsgToAccount succeeded item without sigId defined : ${item.toString}""")
                  None
                case _ => None
              }
              resMap.put(sigIdForKey, (ListBuffer.empty[AccountTransfer], minerFee))
            }
            resMap(sigIdForKey)._1.append(info)
          case _ =>
        }
    }
    resMap
  }

  protected def item2BitwayInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = {
    item.status match {
      case Some(Confirming) =>
        item2CryptoCurrencyTransferInfo(item)
      case _ =>
        logger.warning(s"""${"~" * 50} item2BitwayInfo() get unexpected item : ${item.toString}""")
        None
    }
  }

  protected def item2CryptoCurrencyTransferInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = None

  protected def item2AccountTransfer(item: CryptoCurrencyTransferItem): Option[AccountTransfer] = {
    item.status match {
      case Some(Succeeded) => // batch Set miner fee before sent message to accountProcessor
        transferHandler.get(item.accountTransferId.get)
      case Some(Failed) if item.txType.get != UserToHot && item.txType.get != ColdToHot => //UserToHot fail will do nothing
        transferHandler.get(item.accountTransferId.get)
      case _ =>
        logger.warning(s"""${"~" * 50} item2AccountTransfer() meet unexpected item : ${item.toString}""")
        None
    }
  }

  protected def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler

  protected def handleSucceeded(itemId: Long) {
    id2HandlerMap.remove(itemId) match {
      case Some(handler) =>
        handler.onSucceeded()
        succeededId2HandlerMap.put(itemId, handler)
        msgBoxMap.put(handler.item.id, handler.item)
      case _ =>
    }
  }

  protected def handleFailed(handler: CryptoCurrencyTransferHandler) {}

  protected def updateSigId2MinerFee(tx: CryptoCurrencyTransaction) {
    tx.minerFee match {
      case Some(fee) if tx.sigId.isDefined =>
        tx.status match {
          case Failed => sigId2MinerFeeMap.remove(tx.sigId.get)
          case _ => sigId2MinerFeeMap.put(tx.sigId.get, fee)
        }
      case Some(_) =>
        logger.error(s"""${"~" * 50} updateSigId2MinerFee minerFee defined without sigId defined : ${tx.toString}""")
      case None =>
    }
  }

  protected def refreshLastBlockHeight(currency: Currency, tx: CryptoCurrencyTransaction) {
    val txHeight: Long = if (tx.includedBlock.isDefined) tx.includedBlock.get.height.getOrElse(0L) else 0L
    if (manager.getLastBlockHeight(currency) < txHeight) manager.setLastBlockHeight(currency, txHeight)
  }

}

trait CryptoCurrencyTransferDepositLikeBase extends CryptoCurrencyTransferBase {
  val sigIdWithTxPort2HandlerMap = Map.empty[String, Map[CryptoCurrencyTransactionPort, CryptoCurrencyTransferHandler]]

  override def handleSucceeded(itemId: Long) {
    if (id2HandlerMap.contains(itemId)) {
      val item = id2HandlerMap(itemId).item
      removeItemHandlerFromMap(item.sigId.get, item.to.get)
    }
    super.handleSucceeded(itemId)
  }

  override def handleFailed(handler: CryptoCurrencyTransferHandler) {
    handler.onFail()
    id2HandlerMap.remove(handler.item.id)
    removeItemHandlerFromMap(handler.item.sigId.get, handler.item.to.get)
  }

  override def loadSnapshotItems(snapshotItems: collection.Map[Long, CryptoCurrencyTransferItem]) {
    super.loadSnapshotItems(snapshotItems)
    sigIdWithTxPort2HandlerMap.clear()
    snapshotItems.values map {
      item =>
        if (!sigIdWithTxPort2HandlerMap.contains(item.sigId.get)) {
          sigIdWithTxPort2HandlerMap.put(item.sigId.get, Map.empty[CryptoCurrencyTransactionPort, CryptoCurrencyTransferHandler])
        }
        sigIdWithTxPort2HandlerMap(item.sigId.get).put(item.to.get, id2HandlerMap(item.id))
    }
  }

  override def innerHandleTx(currency: Currency, tx: CryptoCurrencyTransaction) {
    tx.outputs match {
      case Some(outputList) if outputList.size > 0 =>
        outputList filter (out => out.userId.isDefined && out.userId.get != COLD_UID) foreach {
          //ColdToHot transfer should ignore cold outputPort
          outputPort =>
            tx.status match {
              case Failed =>
                getItemHandlerFromMap(tx.sigId.get, outputPort) match {
                  case Some(handler) =>
                    handleFailed(handler)
                  case None =>
                }
              case _ =>
                getItemHandlerFromMap(tx.sigId.get, outputPort) match {
                  case Some(handler) =>
                    handler.onNormal(tx)
                  case _ =>
                    val handler: Option[CryptoCurrencyTransferHandler] =
                      tx.txType match {
                        case Some(Deposit) =>
                          Some(new CryptoCurrencyTransferDepositHandler(currency, outputPort, tx))
                        case Some(ColdToHot) if outputPort.userId.get == HOT_UID =>
                          Some(new CryptoCurrencyTransferColdToHotHandler(currency, outputPort, tx))
                        case _ =>
                          logger.error(s"""${"~" * 50} innerHandleTx() ${tx.txType.get.toString} tx is not valid txType : ${tx.toString}""")
                          None
                      }
                    handler match {
                      case Some(hd) =>
                        saveItemHandlerToMap(tx.sigId.get, outputPort, hd)
                        id2HandlerMap.put(hd.item.id, hd)
                      case _ =>
                    }
                }
            }
        }
        updateSigId2MinerFee(tx)
      case _ =>
        logger.warning(s"""${"~" * 50} innerHandleTx() ${tx.txType.get.toString} tx not define outputs : ${tx.toString}""")
    }
  }

  protected def removeItemHandlerFromMap(sigId: String, port: CryptoCurrencyTransactionPort) {
    if (sigIdWithTxPort2HandlerMap.contains(sigId)) {
      sigIdWithTxPort2HandlerMap(sigId).remove(port)
      if (sigIdWithTxPort2HandlerMap(sigId).isEmpty) {
        sigIdWithTxPort2HandlerMap.remove(sigId)
      }
    }
  }

  private def getItemHandlerFromMap(sigId: String, port: CryptoCurrencyTransactionPort): Option[CryptoCurrencyTransferHandler] = {
    if (sigIdWithTxPort2HandlerMap.contains(sigId) && sigIdWithTxPort2HandlerMap(sigId).contains(port))
      Some(sigIdWithTxPort2HandlerMap(sigId)(port))
    else
      None
  }

  private def saveItemHandlerToMap(sigId: String, port: CryptoCurrencyTransactionPort, handler: CryptoCurrencyTransferHandler) {
    if (!sigIdWithTxPort2HandlerMap.contains(sigId)) {
      sigIdWithTxPort2HandlerMap.put(sigId, Map.empty[CryptoCurrencyTransactionPort, CryptoCurrencyTransferHandler])
    }
    sigIdWithTxPort2HandlerMap(sigId).put(port, handler)
  }
}

trait CryptoCurrencyTransferWithdrawalLikeBase extends CryptoCurrencyTransferBase {

  def newHandlerFromAccountTransfer(t: AccountTransfer, from: Option[CryptoCurrencyTransactionPort], to: Option[CryptoCurrencyTransactionPort]) {
    val handler = new CryptoCurrencyTransferWithdrawalLikeHandler(t, from, to)
    msgBoxMap.put(handler.item.id, handler.item)
    id2HandlerMap.put(handler.item.id, handler)
  }

  override def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler = {
    new CryptoCurrencyTransferWithdrawalLikeHandler(item)
  }

  override def handleFailed(handler: CryptoCurrencyTransferHandler) {
    handler.onFail()
    val item = id2HandlerMap.remove(handler.item.id).get.item
    msgBoxMap.put(item.id, item)
  }

  override def innerHandleTx(currency: Currency, tx: CryptoCurrencyTransaction) {
    tx.ids match {
      case Some(idList) if idList.size > 0 =>
        idList foreach {
          //every input corresponds to one tx
          id =>
            if (id2HandlerMap.contains(id)) {
              tx.status match {
                case Failed =>
                  handleFailed(id2HandlerMap(id))
                case _ =>
                  id2HandlerMap(id).onNormal(tx)
              }
            } else {
              logger.warning(s"""${"~" * 50} innerHandlerTx() ${tx.txType.get.toString} item id ${id} not contained in id2HandlerMap : ${tx.toString}""")
            }
        }
        updateSigId2MinerFee(tx)
      case _ =>
        logger.warning(s"""${"~" * 50} innerHandlerTx() ${tx.txType.get.toString} tx not define ids : ${tx.toString}""")
    }
  }
}