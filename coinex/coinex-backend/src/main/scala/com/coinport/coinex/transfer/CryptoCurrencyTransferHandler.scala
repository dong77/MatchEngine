package com.coinport.coinex.transfer

import akka.event.LoggingAdapter
import com.coinport.coinex.common.mongo.SimpleJsonMongoCollection
import com.coinport.coinex.data._
import com.coinport.coinex.data.TransferStatus._
import com.coinport.coinex.data.TransferType._

trait CryptoCurrencyTransferHandler {
  var manager: AccountTransferManager = null
  var transferHandler: SimpleJsonMongoCollection[AccountTransfer, AccountTransfer.Immutable] = null
  var transferItemHandler: SimpleJsonMongoCollection[CryptoCurrencyTransferItem, CryptoCurrencyTransferItem.Immutable] = null
  var logger: LoggingAdapter = null
  var confirmableHeight = collection.immutable.Map.empty[Currency, Int]
  val defaultConfirmableHeight = 2
  var succeededRetainHeight = collection.immutable.Map.empty[Currency, Int]
  var defaultSucceededRetainHeight = 100
  var item: CryptoCurrencyTransferItem = null

  def setEnv(env: TransferEnv) {
    manager = env.manager
    transferHandler = env.transferHandler
    transferItemHandler = env.transferItemHandler
    logger = env.logger
    confirmableHeight = env.confirmableHeight
    succeededRetainHeight = env.succeededRetainHeight
  }

  def onNormal(tx: CryptoCurrencyTransaction) {
    item.includedBlock match {
      case Some(_) =>
      case None =>
        item = item.copy(sigId = tx.sigId, txid = tx.txid, includedBlock = tx.includedBlock, status = Some(Confirming), updated = Some(System.currentTimeMillis()), minerFee = tx.minerFee)
        setAccountTransferStatus(Confirming)
        saveItemToMongo()
    }
  }

  def onSucceeded() {
    item = item.copy(status = Some(Succeeded), updated = Some(System.currentTimeMillis()))
    setAccountTransferStatus(Succeeded)
    saveItemToMongo()
  }

  def onFail() {
    item = item.copy(status = Some(Failed), updated = Some(System.currentTimeMillis()))
    setAccountTransferStatus(Failed)
    saveItemToMongo()
  }

  def checkConfirm(lastBlockHeight: Long): Boolean = {
    if (item.includedBlock.isDefined && item.status.get != Succeeded && item.status.get != Confirmed) {
      val confirmed = lastBlockHeight - item.includedBlock.get.height.getOrElse(Long.MaxValue) >= itemComfirmableHeight - 1
      if (confirmed) {
        val statusUpdate = if (item.txType.get != Deposit) Succeeded else Confirmed
        item = item.copy(status = Some(statusUpdate))
        setAccountTransferStatus(statusUpdate)
        saveItemToMongo()
      }
      updateAccountTransferConfirmNum(lastBlockHeight)
      confirmed
    } else {
      false
    }
  }

  def checkRemoveSucceeded(lastBlockHeight: Long): Boolean = {
    item.status.get == Succeeded && (lastBlockHeight - item.includedBlock.get.height.get) > itemSucceededRetainHeight
  }

  def reOrgnize(reOrgHeight: Long) {
    if (item.includedBlock.isDefined && item.includedBlock.get.height.isDefined) {
      val itemHeight = item.includedBlock.get.height.get

      // reset item which has bigger height than reOrg's height
      def setReorg() {
        // Confirmed, Reorging
        val newBlock = if (reOrgHeight < itemHeight) None else item.includedBlock
        item = item.copy(includedBlock = newBlock, status = Some(Reorging))
        saveItemToMongo()
      }

      item.status match {
        case Some(Confirming) if reOrgHeight < itemHeight =>
          logger.warning(s"reOrgnize() reOrgnize happened(Confirming) :item -> ${item.toString()}")
          item = item.copy(includedBlock = None)
          saveItemToMongo()
        case Some(Confirmed) if reOrgHeight - itemHeight < itemComfirmableHeight - 1 =>
          logger.warning(s"reOrgnize() reOrgnize happened(Confirmed) :item -> ${item.toString()}")
          setReorg()
        case Some(Reorging) if reOrgHeight < itemHeight =>
          logger.warning(s"reOrgnize() reOrgnize happened(Reorging) :item -> ${item.toString()}")
          setReorg()
        case Some(Succeeded) => //Succeeded item has mv to manager.succeededMap, no need to reorging
        case None =>
        case _ =>
      }

    }
  }

  def reOrgnizeSucceeded(reOrgHeight: Long): Boolean = {
    if (reOrgHeight - item.includedBlock.get.height.get < itemComfirmableHeight - 1) {
      logger.warning(s"reOrgnize() reOrgnize happened(Succeeded) :item -> ${item.toString()}")
      setAccountTransferStatus(Reorging)
      return true
    }
    false
  }

  protected def saveItemToMongo() {
    logger.info("saveItemToMongo : " + item.toString)
    transferItemHandler.put(item.copy(updated = Some(System.currentTimeMillis())))
  }

  private def setAccountTransferStatus(status: TransferStatus) {
    item.accountTransferId foreach {
      accountTransferId =>
        transferHandler.get(accountTransferId) foreach {
          transfer =>
            transferHandler.put(transfer.copy(status = status, updated = Some(System.currentTimeMillis()), txid = item.txid))
        }
    }
  }

  private def updateAccountTransferConfirmNum(lastBlockHeight: Long) {
    if (item.includedBlock.isDefined && item.includedBlock.get.height.isDefined && lastBlockHeight >= item.includedBlock.get.height.get) {
      item.accountTransferId foreach {
        accountTransferId =>
          transferHandler.get(accountTransferId) foreach {
            transfer =>
              transferHandler.put(transfer.copy(confirm = Some(lastBlockHeight - item.includedBlock.get.height.get + 1), updated = Some(System.currentTimeMillis()), txid = item.txid))
          }
      }
    }
  }

  private def itemComfirmableHeight(): Int = {
    confirmableHeight.getOrElse(item.currency, defaultConfirmableHeight)
  }

  private def itemSucceededRetainHeight(): Int = {
    succeededRetainHeight.getOrElse(item.currency, defaultSucceededRetainHeight)
  }

}
