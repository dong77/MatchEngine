/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.bitway

import com.coinport.coinex.data.Currency

final case class BitwayConfig(
  ip: String = "bitway",
  port: Int = 6379,
  batchFetchAddressNum: Int = 100,
  requestChannelPrefix: String = "creq_",
  responseChannelPrefix: String = "cres_",
  maintainedChainLength: Int = 20,
  coldAddresses: List[String] = Nil)

final case class BitwayConfigs(
  configs: Map[Currency, BitwayConfig] = Map.empty)
