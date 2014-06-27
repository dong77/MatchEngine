/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.bitway

import akka.event.LoggingReceive
import akka.persistence.Persistent

import com.coinport.coinex.common.ExtendedView
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import Implicits._

class BitwayView(supportedCurrency: Currency, config: BitwayConfig) extends ExtendedView with BitwayManagerBehavior {
  override val processorId = BITWAY_PROCESSOR << supportedCurrency
  override val viewId = BITWAY_VIEW << supportedCurrency
  val manager = new BitwayManager(supportedCurrency, config.maintainedChainLength, config.coldAddresses)

  def receive = LoggingReceive {
    case Persistent(msg, _) => updateState(msg)
    case QueryCryptoCurrencyAddressStatus(currency, addressType) =>
      sender ! QueryCryptoCurrencyAddressStatusResult(currency, manager.getAddressStatus(addressType))
    case QueryCryptoCurrencyNetworkStatus(currency) =>
      sender ! QueryCryptoCurrencyNetworkStatusResult(currency, manager.getNetworkStatus)
    case QueryReserveStatus(currency) =>
      sender ! QueryReserveStatusResult(currency, manager.getReserveAmounts)
  }
}
