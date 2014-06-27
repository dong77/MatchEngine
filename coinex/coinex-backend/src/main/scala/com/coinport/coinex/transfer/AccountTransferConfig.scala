package com.coinport.coinex.transfer

import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Currency

class AccountTransferConfig(
  val transferDebug: Boolean = true,
  val confirmableHeight: collection.Map[Currency, Int] = collection.Map(Btc -> 2, Ltc -> 3, Doge -> 4),
  val succeededRetainHeight: collection.Map[Currency, Int] = collection.Map(Btc -> 100, Ltc -> 200, Doge -> 300))
