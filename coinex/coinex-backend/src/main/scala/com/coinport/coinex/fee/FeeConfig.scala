/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.fee
import com.coinport.coinex.data.MarketSide
import com.coinport.coinex.data.Currency
import com.coinport.coinex.data.TFeeConfig

final case class FeeConfig(
    marketFeeRules: Map[MarketSide, FeeRule],
    robotFeeRules: Map[Int, FeeRule],
    transferFeeRules: Map[Currency, FeeRule],
    freeOfTxChargeUserIdThreshold: Long = 0L) {

  def toThrift = TFeeConfig(
    marketFeeRules = marketFeeRules.map(kv => (kv._1 -> kv._2.toThrift)),
    robotFeeRules = robotFeeRules.map(kv => (kv._1 -> kv._2.toThrift)),
    transferFeeRules = transferFeeRules.map(kv => (kv._1 -> kv._2.toThrift))
  )
}
