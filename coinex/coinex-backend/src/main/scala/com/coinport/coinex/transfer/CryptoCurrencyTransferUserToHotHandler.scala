package com.coinport.coinex.transfer

import com.coinport.coinex.data._
import com.coinport.coinex.data.TransferStatus._
import com.coinport.coinex.data.TransferType.UserToHot

object CryptoCurrencyTransferUserToHotHandler extends CryptoCurrencyTransferBase {

  override def innerHandleTx(currency: Currency, tx: CryptoCurrencyTransaction) {
    tx.ids match {
      case Some(idList) if idList.size > 0 =>
        idList foreach {
          id =>
            if (id2HandlerMap.contains(id)) {
              tx.status match {
                case Failed =>
                  handleFailed(id2HandlerMap(id))
                case _ =>
                  id2HandlerMap(id).onNormal(tx)
              }
            } else {
              logger.warning(s"""${"~" * 50} innerHandleTx() UserToHot item confirm id contained in id2HandlerMap : ${tx.toString}""")
            }
        }
        updateSigId2MinerFee(tx)
      case _ =>
        logger.warning(s"""${"~" * 50} innerHandleTx() UserToHot tx not define ids : ${tx.toString}""")
    }
  }

  override def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler = {
    new CryptoCurrencyTransferUserToHotHandler(item)
  }

  override def handleFailed(handler: CryptoCurrencyTransferHandler) {
    handler.onFail()
    id2HandlerMap.remove(handler.item.id)
  }

  override def item2CryptoCurrencyTransferInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = {
    Some(CryptoCurrencyTransferInfo(item.id, None, item.from.get.internalAmount, item.from.get.amount, Some(item.from.get.address)))
  }

  def createUserToHot(depositItem: CryptoCurrencyTransferItem) {
    val handler = new CryptoCurrencyTransferUserToHotHandler(depositItem)
    id2HandlerMap.put(handler.item.id, handler)
    msgBoxMap.put(handler.item.id, handler.item)
  }
}

class CryptoCurrencyTransferUserToHotHandler extends CryptoCurrencyTransferHandler {
  def this(item: CryptoCurrencyTransferItem)(implicit env: TransferEnv) {
    this()
    setEnv(env)
    item.txType match {
      case Some(TransferType.UserToHot) =>
        this.item = item
      case Some(TransferType.Deposit) =>
        createItemFromDepositItem(item)
      case _ =>
        logger.error(s"""${"~" * 50} Try create UserToHot with not userToHot nor deposit item : ${item.toString}""")
    }
  }

  private def createItemFromDepositItem(depositItem: CryptoCurrencyTransferItem) {
    val transferId = manager.getTransferId
    manager.setLastTransferId(transferId)
    transferHandler.put(AccountTransfer(transferId, depositItem.userId.get, TransferType.UserToHot, depositItem.currency, depositItem.to.get.internalAmount.get, Confirming, Some(System.currentTimeMillis()), address = Some(depositItem.to.get.address)))
    // id, currency, sigId, txid, userId, from, to(hot address), includedBlock, txType, status, userToHotMapedDepositId, accountTransferId, created, updated
    item = CryptoCurrencyTransferItem(manager.getNewTransferItemId, depositItem.currency, None, None, depositItem.userId, depositItem.to, None, None, Some(UserToHot), Some(Confirming), Some(depositItem.id), Some(transferId), Some(System.currentTimeMillis()))
    saveItemToMongo()
  }

  override def onFail() {
    super.onFail()
    updateDepositTx()
  }

  override def onNormal(tx: CryptoCurrencyTransaction) {
    super.onNormal(tx)
    updateDepositTx()
  }

  private def updateDepositTx() {
    item.userToHotMapedDepositId match {
      case Some(depositId) =>
        CryptoCurrencyTransferDepositHandler.updateByUserToHot(depositId, item.status.get)
      case None =>
        logger.error(s"""${"~" * 50} updateDepositTx() UserToHot item not define userToHotMapedDepositId : ${item.toString}""")
    }
  }

}