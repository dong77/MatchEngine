/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.common

import akka.persistence._
import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import scala.concurrent.duration._
import com.coinport.coinex.data.TakeSnapshotNow
import com.coinport.coinex.common.support._

trait ExtendedProcessor extends ExtendedActor with SnapshotSupport with ChannelSupport with RecoverySupport {

  def identifyChannel: PartialFunction[Any, String] = PartialFunction.empty

  val snapshotIntervalSec = 60 * 60 // default to 1 hour

  override def preStart() = {
    log.info("============ processorId: {}", processorId)
    super.preStart
  }

  override def confirm(p: ConfirmablePersistent) {
    p.confirm()
  }

  override def onRecoveryFinish() = {
    log.info("============ recovery finished: {}", processorId)
  }
}
