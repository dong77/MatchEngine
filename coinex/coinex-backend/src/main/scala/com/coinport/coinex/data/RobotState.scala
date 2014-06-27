/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.data

import scala.collection.immutable.SortedMap
import scala.collection.immutable.SortedSet

import org.slf4s.Logging

import com.coinport.coinex.common.Constants.Action
import com.coinport.coinex.robot.RobotDNA
import com.coinport.coinex.util.MHash
import com.twitter.util.Eval

object RobotState {
  implicit val ordering = new Ordering[Robot] {
    def compare(lhs: Robot, rhs: Robot) = if (lhs.robotId < rhs.robotId) -1 else if (lhs.robotId > rhs.robotId) 1 else 0
  }

  val EmptyRobotPool = SortedSet.empty[Robot]

}

case class RobotState(
    robotPool: SortedSet[Robot] = RobotState.EmptyRobotPool,
    robotMap: Map[Long, Robot] = Map.empty[Long, Robot],
    metrics: Metrics = Metrics(),
    robotDNAMap: Map[Long, RobotDNA] = Map.empty[Long, RobotDNA]) extends Object with Logging {

  def getRobot(id: Long): Option[Robot] = robotMap.get(id)

  def getRobotPool = robotPool

  def addRobot(robot: Robot): RobotState = {
    log.debug("[ADD ROBOT] robotId: %d, dnaId: %s".format(robot.robotId, robot.dnaId))
    copy(robotPool = robotPool + robot, robotMap = robotMap + (robot.robotId -> robot))
  }

  def addRobotDNA(states: scala.collection.immutable.Map[String, String]): (Long, RobotState) = {
    var stateAction: Map[String, Action] = states map { state =>
      (state._1 -> inflate(state._2))
    }
    val dnaId = genDNAId(states)

    if (robotDNAMap.contains(dnaId)) {
      log.debug("[EXIST ROBOT DNA] id: %d".format(dnaId))
      (dnaId, this)
    } else {
      log.debug("[ADD ROBOT DNA] id: %d, state: %s".format(robotDNAMap.size, stateAction.keySet.mkString(",")))
      (dnaId, copy(robotDNAMap = robotDNAMap + (dnaId -> RobotDNA(dnaId, stateAction, states))))
    }
  }

  def removeRobot(rid: Long): RobotState = {
    robotMap.get(rid) match {
      case Some(robot) =>
        copy(robotPool = robotPool - robot, robotMap = robotMap - robot.robotId)
      case _ => this
    }
  }

  def updateMetrics(m: Metrics): RobotState = {
    copy(metrics = m)
  }

  def inflate(source: String): Action = {

    val HEADER = """
      import com.coinport.coinex.data._
      import com.coinport.coinex.data.Currency._
      (robot: Robot, metrics: Option[Metrics]) =>

    """
    (new Eval()(HEADER + source)).asInstanceOf[Action]
  }

  def isExistRobotDNA(states: scala.collection.immutable.Map[String, String]): (Long, Boolean) = {
    val dnaId = genDNAId(states)
    robotDNAMap.contains(dnaId)
    (dnaId, robotDNAMap.contains(dnaId))
  }

  def getUsingRobots(dnaId: Long): SortedSet[Long] = {

    var robots: SortedSet[Long] = SortedSet.empty[Long]
    robotPool foreach {
      robot => if (dnaId == robot.dnaId) robots += robot.robotId
    }
    robots
  }

  def removeRobotDNA(dnaId: Long): RobotState = {

    if (getUsingRobots(dnaId).size > 0) {
      this
    } else {
      copy(robotDNAMap = robotDNAMap - dnaId)
    }
  }

  private def genDNAId(states: scala.collection.immutable.Map[String, String]): Long = {
    MHash.murmur3((SortedMap[String, String]() ++ states).toString)
  }

  def toThrift: TRobotState = {
    TRobotState.apply(
      robotPool.map(rbt => rbt.toThrift),
      robotMap.map(kv => (kv._1 -> kv._2.toThrift)),
      metrics,
      robotDNAMap.map(kv => (kv._1 -> kv._2.dnaStringMap)))
  }

  def fromThrift(tRobotState: TRobotState): RobotState = {
    val rdsM = tRobotState.robotDNAMap.map(kv =>
      (kv._1 ->
        RobotDNA(
          kv._1,
          kv._2.map(state => (state._1 -> inflate(state._2))).toMap,
          kv._2.map(kv => (kv._1, kv._2)).toMap
        )
      )
    )

    RobotState(
      RobotState.EmptyRobotPool ++ tRobotState.robotPool.map(rbt => Robot.fromThrift(rbt)),
      tRobotState.robotMap.map(kv => (kv._1 -> Robot.fromThrift(kv._2))).map(kv => (kv._1, kv._2)).toMap,
      tRobotState.metrics,
      rdsM.map(kv => (kv._1, kv._2)).toMap)
  }
}
