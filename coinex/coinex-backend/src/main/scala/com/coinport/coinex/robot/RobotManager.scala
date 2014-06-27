/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.robot

import com.coinport.coinex.data._
import com.coinport.coinex.common.Constants._
import com.coinport.coinex.common.Manager
import Implicits._
import scala.collection.immutable.SortedSet

class RobotManager extends Manager[TRobotState] {

  var state = RobotState()

  override def getSnapshot = {
    state.toThrift
  }

  override def loadSnapshot(s: TRobotState) {
    state = state.fromThrift(s)
  }

  def apply() = state

  def addRobot(robot: Robot) {
    state = state.addRobot(robot)
  }

  def removeRobot(id: Long): Option[Robot] = {
    val robot = state.getRobot(id)
    robot foreach {
      _ => state = state.removeRobot(id)
    }
    robot
  }

  def addRobotDNA(states: scala.collection.immutable.Map[String, String]): Long = {
    val (dnaId, resultState) = state.addRobotDNA(states)
    state = resultState
    dnaId
  }

  def removeRobotDNA(dnaId: Long) {
    state = state.removeRobotDNA(dnaId)
  }

  def isExistRobotDNA(states: scala.collection.immutable.Map[String, String]): (Long, Boolean) = {
    state.isExistRobotDNA(states)
  }

  def getUsingRobots(dnaId: Long): SortedSet[Long] = {
    state.getUsingRobots(dnaId)
  }

  def getAction(dnaId: Long, currState: String): Action = {
    state.robotDNAMap(dnaId).dna(currState)
  }

  def updateMetrics(m: Metrics) {
    state = state.updateMetrics(m)
  }
}
