package com.coinport.coinex.transfer

import com.coinport.coinex.data._
import com.coinport.coinex.data.TransferStatus._

object CryptoCurrencyTransferDepositHandler extends CryptoCurrencyTransferDepositLikeBase {

  // Deposit shouldn't send message to bitway
  override def getMsgToBitway(currency: Currency): List[CryptoCurrencyTransferInfo] = {
    List.empty[CryptoCurrencyTransferInfo]
  }

  override def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler = {
    new CryptoCurrencyTransferDepositHandler(item)
  }

  def updateByUserToHot(depositId: Long, userToHotStatus: TransferStatus) {
    // check coresponded deposit item has not been removed from map
    if (id2HandlerMap.contains(depositId)) {
      userToHotStatus match {
        case Failed =>
          handleFailed(id2HandlerMap(depositId))
        case _ =>
          handleSucceeded(depositId)
      }
    }
  }

}

class CryptoCurrencyTransferDepositHandler(currency: Currency, outputPort: CryptoCurrencyTransactionPort, tx: CryptoCurrencyTransaction)(implicit env: TransferEnv)
    extends CryptoCurrencyTransferDepositLikeHandler(currency, outputPort, tx) {
  setEnv(env)

  def this(item: CryptoCurrencyTransferItem)(implicit env: TransferEnv) {
    this(null, null, null)
    this.item = item
  }

  override def checkConfirm(lastBlockHeight: Long): Boolean = {
    //Reorging item will not confirm again to avoid resend UserToHot message
    if (super.checkConfirm(lastBlockHeight) && item.status.get != Reorging) {
      CryptoCurrencyTransferUserToHotHandler.createUserToHot(item)
      return true
    }
    false
  }

}

object CryptoCurrencyTransferHotToColdHandler extends CryptoCurrencyTransferWithdrawalLikeBase {

  override def item2CryptoCurrencyTransferInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = {
    Some(CryptoCurrencyTransferInfo(item.id, None, item.to.get.internalAmount, item.to.get.amount, None))
  }

}

object CryptoCurrencyTransferUnknownHandler extends CryptoCurrencyTransferBase {

  override def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler = {
    null
  }

  override def handleTx(currency: Currency, tx: CryptoCurrencyTransaction) {
    refreshLastBlockHeight(currency, tx)
  }

}

object CryptoCurrencyTransferWithdrawalHandler extends CryptoCurrencyTransferWithdrawalLikeBase {

  override def item2CryptoCurrencyTransferInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = {
    Some(CryptoCurrencyTransferInfo(item.id, Some(item.to.get.address), item.to.get.internalAmount, item.to.get.amount, None))
  }

}