package com.coinport.coinex.robot

import org.specs2.mutable._
import com.coinport.coinex.data.RobotState
import com.coinport.coinex.data._
import com.coinport.coinex.robot.sample.StopOrderRobot
import com.coinport.coinex.data.Currency._
import Implicits._

class RobotStateSpec extends Specification {

  "robot state" should {
    "add and remove robot dna" in {

      val robotState = new RobotState(
        RobotState.EmptyRobotPool,
        Map.empty[Long, Robot],
        Metrics(),
        Map.empty[Long, RobotDNA])

      val (payload, dna) = StopOrderRobot(10, 2, 10000, 3125.0, (Btc ~> Cny), Order(1, 1, 2, Some(3429.0)))
      val (dnaId, robotState2) = robotState.addRobotDNA(dna)

      robotState2.robotDNAMap.contains(dnaId) mustEqual true
      robotState2.isExistRobotDNA(dna)._2 mustEqual true

      val (dnaId2, robotState3) = robotState2.addRobotDNA(dna)
      dnaId2 mustEqual dnaId
      robotState3.getUsingRobots(dnaId).contains(10) mustEqual false
      robotState3.isExistRobotDNA(dna)._2 mustEqual true

      val robotState4 = robotState3.addRobot(Robot(10, 1, 1, dnaId = dnaId, payloads = payload))
      robotState4.getUsingRobots(dnaId).contains(10) mustEqual true

      val robotState5 = robotState4.removeRobotDNA(dnaId)
      robotState5.robotDNAMap.contains(dnaId) mustEqual true
      val robotState6 = robotState5.removeRobot(10)
      robotState6.getUsingRobots(dnaId).contains(10) mustEqual false
      val robotState7 = robotState6.removeRobotDNA(dnaId)
      robotState7.robotDNAMap.contains(dnaId) mustEqual false
    }

    "robotState serialize from and to thrift" in {
      val robotState = new RobotState(
        RobotState.EmptyRobotPool,
        Map.empty[Long, Robot],
        Metrics(),
        Map.empty[Long, RobotDNA])

      val (payload, dna) = StopOrderRobot(10, 2, 10000, 3125.0, (Btc ~> Cny), Order(1, 1, 2, Some(3429.0)))
      val (dnaId, robotState2) = robotState.addRobotDNA(dna)

      robotState2.robotDNAMap.contains(dnaId) mustEqual true
      robotState2.isExistRobotDNA(dna)._2 mustEqual true

      val (dnaId2, robotState3) = robotState2.addRobotDNA(dna)
      dnaId2 mustEqual dnaId
      robotState3.getUsingRobots(dnaId).contains(10) mustEqual false
      robotState3.isExistRobotDNA(dna)._2 mustEqual true

      val robotState4 = robotState3.addRobot(Robot(10, 1, 1, dnaId = dnaId, payloads = payload))
      robotState4.getUsingRobots(dnaId).contains(10) mustEqual true
      val tRobotState = robotState4.toThrift
      val robotState5 = robotState4.fromThrift(tRobotState)
      robotState5.robotDNAMap(2208786710083266701L).dnaId mustEqual 2208786710083266701L

    }

  }

}