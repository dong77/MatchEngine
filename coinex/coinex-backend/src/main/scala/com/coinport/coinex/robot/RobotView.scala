/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.robot

import akka.event.LoggingReceive
import com.coinport.coinex.common.ExtendedView
import akka.persistence.Persistent
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import Implicits._

class RobotView extends ExtendedView {
  override val processorId = ROBOT_PROCESSOR <<
  override val viewId = ROBOT_VIEW <<
  val manager = new RobotManager()
  def receive = LoggingReceive {
    case _ =>
  }
}
