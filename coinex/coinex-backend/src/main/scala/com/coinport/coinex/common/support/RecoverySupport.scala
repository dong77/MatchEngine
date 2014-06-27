/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.common.support

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._

import com.coinport.coinex.data.QueryRecoverStats

trait RecoverySupport extends Actor with ActorLogging {
  final val INTERVAL = 1

  abstract override def preStart() = {
    super.preStart()
    scheduleQueryRecoverStats()
  }

  def onRecoveryFinish()

  def execAfterRecover(recoveryFinished: Boolean) = {
    if (recoveryFinished)
      onRecoveryFinish()
    else
      scheduleQueryRecoverStats()
  }

  private def scheduleQueryRecoverStats() = {
    context.system.scheduler.scheduleOnce(INTERVAL seconds, self, QueryRecoverStats)(context.system.dispatcher)
  }
}
