/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.markets

import akka.event.LoggingReceive
import akka.persistence._

import com.coinport.coinex.common.ExtendedProcessor
import com.coinport.coinex.common.SimpleManager
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import Implicits._

// This is an empty processor to support various views.
class MarketUpdateProcessor extends ExtendedProcessor with Processor {
  override def processorId = MARKET_UPDATE_PROCESSOR <<

  val manager = new SimpleManager()

  override def identifyChannel: PartialFunction[Any, String] = {
    case _ => "ap"
  }

  def receive = LoggingReceive {
    case p: ConfirmablePersistent => confirm(p)
    case _ =>
  }
}

