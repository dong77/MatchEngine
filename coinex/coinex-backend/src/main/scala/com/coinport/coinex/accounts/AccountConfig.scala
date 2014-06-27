/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.accounts

import com.coinport.coinex.fee.FeeConfig
import com.coinport.coinex.data.Currency

final case class HotColdTransferStrategy(high: Double, low: Double)

final case class AccountConfig(
  feeConfig: FeeConfig,
  hotColdTransfer: Map[Currency, HotColdTransferStrategy],
  enableHotColdTransfer: Boolean = false,
  hotColdTransferInterval: Long = 0L)
