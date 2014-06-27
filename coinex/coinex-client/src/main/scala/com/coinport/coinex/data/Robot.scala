/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.data

import com.twitter.util.Eval
import org.slf4s.Logging
import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import java.nio.ByteBuffer
import scala.util.Marshal
import com.coinport.coinex.util.ScalaSerializer

object Robot {

  def fromByteBuffer(bf: ByteBuffer): Map[String, Option[Any]] = {
    ScalaSerializer.deserialize[Map[String, Option[Any]]](bf.array())
  }

  def fromThrift(tRobot: TRobot): Robot = {
    val payloads = fromByteBuffer(tRobot.payloads)
    Robot(tRobot.robotId,
      tRobot.userId,
      tRobot.timestamp,
      tRobot.currentState,
      tRobot.dnaId,
      payloads)
  }
}

// TODO(c) use cache for state handler instead of inflating it from string every time
// we need change 'robot' in user's code to r inorder to hide the immutable such as:
// if user wrote: robot.setPayload("A", "1") we need change to r = robot.setPayload("A", "1")
case class Robot(
    robotId: Long, userId: Long = COINPORT_UID, timestamp: Long = 0,
    currentState: String = "START",
    dnaId: Long = 0L,
    payloads: Map[String, Option[Any]] = Map.empty[String, Option[Any]]) extends Object with Logging {

  // Option[Any] is the actual action of the robot, such as DoRequestCashDeposit.
  // This could be restrained from outter processor
  // TODO(c): try to make Metrics as T

  private val DONE = "DONE"

  // invoked by outter processor
  def action(metrics: Option[Metrics] = None, actionFunction: Action): (Robot, Option[Any]) = currentState match {
    case DONE => (this, None)
    case cs if (actionFunction != null) =>
      val function = actionFunction
      try {
        function(this, metrics)
      } catch {
        case ex: Throwable =>
          log.error("exception occur in #%d robot's state %s handler" format (this.robotId, currentState), ex)
          (this -> DONE, None)
      }
    case _ =>
      log.error("robot #%d doesn't contain the state %s" format (this.robotId, currentState))
      (this, None)
  }

  def isDone = currentState == DONE

  def getPayload[T](state: String) =
    Option(payloads.getOrElse(state, None).getOrElse(null).asInstanceOf[T])

  def setPayload(state: String, payload: Option[Any]): Robot = {
    copy(payloads = payloads + (state -> payload))
  }

  def ->(state: String): Robot = { copy(currentState = state) }

  private def inflate(source: String): Action = {
    (new Eval()(source)).asInstanceOf[Action]
  }

  def toThrift: TRobot = {
    TRobot(robotId = robotId,
      userId = userId,
      timestamp = timestamp,
      payloads = toByteBuffer(payloads),
      currentState = currentState,
      dnaId = dnaId)
  }

  def toByteBuffer(sp: Map[String, Option[Any]]): ByteBuffer = {
    ByteBuffer.wrap(ScalaSerializer.serialize[Map[String, Option[Any]]](sp))
  }
}
