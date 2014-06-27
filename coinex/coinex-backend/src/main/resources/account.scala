/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

import com.coinport.coinex.accounts._
import com.coinport.coinex.api.model._
import com.coinport.coinex.common._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.fee._
import Constants._
import Implicits._

AccountConfig(
  feeConfig = FeeConfig(
    marketFeeRules = Map(
      (Pts ~> Cny) -> PercentageFee(0.003),
      (Cny ~> Pts) -> PercentageFee(0.003)),

    robotFeeRules = Map(
      TRAILING_STOP_ORDER_ROBOT_TYPE -> PercentageFee(0.003),
      STOP_ORDER_ROBOT_TYPE -> PercentageFee(0.002)),

    transferFeeRules = Map(
      Btc -> ConstantFee(0.0002.internalValue(Btc)),
      Doge -> ConstantFee(0.0002.internalValue(Doge)),
      Cny -> PercentageFee(0.002)),

    freeOfTxChargeUserIdThreshold = 1E9.toLong + 1000 // 1 thousand
    ),

  hotColdTransfer = Map(
    Btc -> HotColdTransferStrategy(0.25, 0.1),
    Doge -> HotColdTransferStrategy(0.25, 0.1)
  ),
  enableHotColdTransfer = false,
  hotColdTransferInterval = 600 * 1000L
)
