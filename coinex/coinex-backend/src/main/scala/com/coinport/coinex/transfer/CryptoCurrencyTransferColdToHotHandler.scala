package com.coinport.coinex.transfer

import com.coinport.coinex.data._
import com.coinport.coinex.common.Constants._
import com.coinport.coinex.api.model.CurrencyWrapper

object CryptoCurrencyTransferColdToHotHandler extends CryptoCurrencyTransferDepositLikeBase {

  override def item2CryptoCurrencyTransferInfo(item: CryptoCurrencyTransferItem): Option[CryptoCurrencyTransferInfo] = {
    Some(CryptoCurrencyTransferInfo(item.id, None, item.to.get.internalAmount, item.to.get.amount, None))
  }

  override def newHandlerFromItem(item: CryptoCurrencyTransferItem): CryptoCurrencyTransferHandler = {
    new CryptoCurrencyTransferColdToHotHandler(item)
  }

}

class CryptoCurrencyTransferColdToHotHandler(currency: Currency, outputPort: CryptoCurrencyTransactionPort, tx: CryptoCurrencyTransaction)(implicit env: TransferEnv)
    extends CryptoCurrencyTransferDepositLikeHandler(currency, outputPort, tx) {
  if (currency != null && outputPort != null && tx != null && tx.minerFee.isDefined) {
    val hotOutputList = tx.outputs.get filter { out => out.userId.isDefined && out.userId.get == HOT_UID }
    println("$" * 50 + "hotOutputList = " + hotOutputList.toString() + ", outputPort = " + outputPort.toString + ", tx.minerFee = " + tx.minerFee.toString)
    val outputSize = hotOutputList.size
    val newInternalAmount: Long = {
      outputPort.address.equals(hotOutputList.last.address) match {
        case true => // last output should take all leaved minerFee after divided by former output
          outputPort.internalAmount.get + (tx.minerFee.get - (tx.minerFee.get / outputSize * (outputSize - 1)))
        case false =>
          outputPort.internalAmount.get + (tx.minerFee.get / outputSize)
      }
    }
    val newAmount = new CurrencyWrapper(newInternalAmount).externalValue(currency)
    item = item.copy(to = Some(item.to.get.copy(internalAmount = Some(newInternalAmount), amount = Some(newAmount))))
    // reset AccountTransfer.amount to newInternalAmount
    transferHandler.put(transferHandler.get(item.accountTransferId.get).get.copy(amount = newInternalAmount))
    saveItemToMongo()
  }

  def this(item: CryptoCurrencyTransferItem)(implicit env: TransferEnv) {
    this(null, null, null)
    this.item = item
  }

}
