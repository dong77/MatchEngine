/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

import com.coinport.coinex.common._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.bitway._
import Constants._
import Implicits._

BitwayConfigs(Map(
  Btc -> BitwayConfig(
    ip = "127.0.0.1",
    port = 6379,
    maintainedChainLength = 10
  ),
  Ltc -> BitwayConfig(
    ip = "127.0.0.1",
    port = 6379,
    maintainedChainLength = 20
  )
))
