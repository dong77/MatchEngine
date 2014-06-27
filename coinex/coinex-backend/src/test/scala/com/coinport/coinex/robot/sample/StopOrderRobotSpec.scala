/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.robot.sample

import org.specs2.mutable._

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import Implicits._
import com.coinport.coinex.common.Constants._

class StopOrderRobotSpec extends Specification {
  "stop order robot" should {
    "stop order robot for sell btc" in {

      val (payload, stateMap) = StopOrderRobot(1, 2, 10000, 3125.0, (Btc ~> Cny), Order(1, 1, 2, Some(3429.0)))

      var actionMap: Map[String, Action] = stateMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }
      actionMap += "DONE" -> new RobotState().inflate("""(robot -> "DONE", None)""")

      var robot = Robot(1, 2, 10000, dnaId = 1L, payloads = payload)

      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))

      res1 mustEqual None
      val (robot2, res2) = robot1.action(None, actionMap(robot1.currentState))
      res2 mustEqual None
      val (robot3, res3) = robot2.action(
        Some(Metrics(Map((Btc ~> Cny) -> MetricsByMarket((Btc ~> Cny), 3421.0)))),
        actionMap(robot2.currentState)
      )
      res3 mustEqual None
      val (robot4, res4) = robot3.action(
        Some(Metrics(Map((Btc ~> Cny) -> MetricsByMarket((Btc ~> Cny), 2000.0)))),
        actionMap(robot3.currentState)
      )
      res4 mustEqual Some(DoSubmitOrder((Btc ~> Cny),
        Order(2, 1, 2, Some(3429.0), robotId = Some(1), robotType = Some(1))))

      robot4.isDone mustEqual true
    }

    "stop order robot for buy btc" in {
      val (payload, stateMap) = StopOrderRobot(1, 2, 10000, 1 / 3125.0, (Cny ~> Btc), Order(1, 1, 2 * 3000, Some(1 / 3000.0)))
      var actionMap: Map[String, Action] = stateMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }
      actionMap += "DONE" -> new RobotState().inflate("""(robot -> "DONE", None)""")
      var robot = Robot(1, 2, 10000, dnaId = 1L, payloads = payload)
      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))
      res1 mustEqual None
      val (robot2, res2) = robot1.action(None, actionMap(robot1.currentState))
      res2 mustEqual None
      val (robot3, res3) = robot2.action(
        Some(Metrics(Map((Cny ~> Btc) -> MetricsByMarket((Cny ~> Btc), 1 / 3111.0)))),
        actionMap(robot2.currentState)
      )
      res3 mustEqual None
      val (robot4, res4) = robot3.action(
        Some(Metrics(Map((Cny ~> Btc) -> MetricsByMarket((Cny ~> Btc), 1 / 4000.0)))),
        actionMap(robot3.currentState)
      )
      res4 mustEqual Some(DoSubmitOrder((Cny ~> Btc),
        Order(2, 1, 2 * 3000, Some(1 / 3000.0), robotId = Some(1), robotType = Some(1))))
      robot4.isDone mustEqual true
    }
  }
}
