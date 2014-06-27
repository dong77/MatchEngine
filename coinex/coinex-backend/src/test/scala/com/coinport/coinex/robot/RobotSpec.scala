/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.robot

import org.specs2.mutable._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.common._
import com.twitter.util.Eval
import com.coinport.coinex.common.Constants._

class RobotSpec extends Specification {
  "Robot" should {
    "simple Robot" in {
      var robot = Robot(1, 1, 1)

      val modelMap: Map[String, String] = Map(("START" -> """(robot -> "STATE_A", None)"""),
        ("STATE_A" -> """(robot -> "STATE_B", Some(1))"""),
        ("STATE_B" -> """(robot -> "STATE_C", Some(2))"""),
        ("STATE_C" -> """(robot -> "DONE", Some(3))"""),
        ("DONE" -> """(robot -> "DONE", None)"""))

      var actionMap: Map[String, Action] = modelMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }

      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))
      res1 mustEqual None
      robot1.isDone mustEqual false
      val (robot2, res2) = robot1.action(None, actionMap(robot1.currentState))
      res2 mustEqual Some(1)
      robot2.isDone mustEqual false
      val (robot3, res3) = robot2.action(None, actionMap(robot2.currentState))
      res3 mustEqual Some(2)
      val (robot4, res4) = robot3.action(None, actionMap(robot3.currentState))
      res4 mustEqual Some(3)
      robot4.isDone mustEqual true
      val (robot5, res5) = robot4.action(None, actionMap(robot4.currentState))
      res5 mustEqual None
      robot5.isDone mustEqual true
    }

    "robot with state payload" in {
      val metricsImpl = Some(Metrics(Map(MarketSide(Btc, Cny) -> MetricsByMarket(MarketSide(Btc, Cny), 123))))
      var robot = Robot(2, 1, 1)

      val modelMap: Map[String, String] = Map(("START" -> """
          var r = robot.setPayload("START", Some(31.9))
          r = r.setPayload("STATE_A", Some(2))
          r = r.setPayload("STATE_B", Some(2))
          (r -> "STATE_A", None)
          """),
        ("STATE_A" -> """
        require(metrics == Some(Metrics(Map(MarketSide(Btc, Cny) -> MetricsByMarket(MarketSide(Btc, Cny), 123)))))
        val payload = robot.getPayload[Int]("STATE_A")
        val r = robot.setPayload("STATE_A", payload map { _ - 1 })
        if (payload.get != 1) {
          (r -> "STATE_A", Some(0))
        } else {
          (r -> "STATE_B", Some(1))
        }
        """),
        ("STATE_B" -> """
        require(metrics == None)
        (robot -> "STATE_C", Some(2))
        """),
        ("STATE_C" -> """
        require(metrics == None)
        val price = robot.getPayload[Double]("START").get
        require(price == 31.9)
        (robot -> "DONE", Some(3))
        """),
        ("DONE" -> """(robot -> "DONE", None)"""))

      var actionMap: Map[String, Action] = modelMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }

      robot.robotId mustEqual 2
      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))
      res1 mustEqual None
      val (robot2, res2) = robot1.action(metricsImpl, actionMap(robot1.currentState))
      res2 mustEqual Some(0)
      val (robot3, res3) = robot2.action(metricsImpl, actionMap(robot2.currentState))
      res3 mustEqual Some(1)
      val (robot4, res4) = robot3.action(None, actionMap(robot3.currentState))
      res4 mustEqual Some(2)
      robot4.isDone mustEqual false
      val (robot5, res5) = robot4.action(None, actionMap(robot4.currentState))
      res5 mustEqual Some(3)
      robot5.isDone mustEqual true
      val (robot6, res6) = robot5.action(None, actionMap(robot5.currentState))
      res6 mustEqual None
    }

    "robot go to bad state" in {
      var robot = Robot(1, 1, 1)

      val modelMap: Map[String, String] = Map(("START" -> """(robot -> "STATE_A", None)"""),
        ("STATE_A" -> """(robot -> "STATE_D", Some(1))"""),
        ("STATE_B" -> """(robot -> "STATE_C", Some(2))"""),
        ("STATE_C" -> """(robot -> "DONE", Some(3))"""),
        ("STATE_D" -> """(robot -> "DONE", None)"""),
        ("DONE" -> """(robot -> "DONE", None)"""))

      var actionMap: Map[String, Action] = modelMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }

      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))
      res1 mustEqual None
      robot1.isDone mustEqual false
      val (robot2, res2) = robot1.action(None, actionMap(robot1.currentState))
      res2 mustEqual Some(1)
      robot2.isDone mustEqual false
      val (robot3, res3) = robot2.action(None, actionMap(robot2.currentState))
      res3 mustEqual None
      robot3.isDone mustEqual true
      val (robot4, res4) = robot3.action(None, actionMap(robot3.currentState))
      res4 mustEqual None
      robot4.isDone mustEqual true
      val (robot5, res5) = robot4.action(None, actionMap(robot4.currentState))
      res5 mustEqual None
      robot5.isDone mustEqual true
    }

    "robot throw Exception in handler" in {
      var robot = Robot(1, 1, 1)

      val modelMap: Map[String, String] = Map(("START" -> """(robot -> "STATE_A", None)"""),
        ("STATE_A" -> """
          throw new Exception()
          (robot -> "STATE_D", Some(1))
        """),
        ("STATE_B" -> """(robot -> "STATE_C", Some(2))"""),
        ("STATE_C" -> """(robot -> "DONE", Some(3))"""),
        ("DONE" -> """(robot -> "DONE", None)"""))

      var actionMap: Map[String, Action] = modelMap map { state =>
        (state._1 -> new RobotState().inflate(state._2))
      }

      val (robot1, res1) = robot.action(None, actionMap(robot.currentState))
      res1 mustEqual None
      robot1.isDone mustEqual false
      val (robot2, res2) = robot1.action(None, actionMap(robot1.currentState))
      res2 mustEqual None
      robot2.isDone mustEqual true
      val (robot3, res3) = robot2.action(None, actionMap(robot2.currentState))
      res3 mustEqual None
      robot3.isDone mustEqual true
      val (robot4, res4) = robot3.action(None, actionMap(robot3.currentState))
      res4 mustEqual None
      robot4.isDone mustEqual true
      val (robot5, res5) = robot4.action(None, actionMap(robot4.currentState))
      res5 mustEqual None
      robot5.isDone mustEqual true
    }

    "robot serialize" in {
      val payload = Map("123" -> Some(1), "222" -> Some("xxx"), "333" -> None)
      var robot = Robot(1, 2, 3, dnaId = 4, payloads = payload)

      val tRobot = robot.toThrift
      tRobot.robotId mustEqual 1
      tRobot.userId mustEqual 2
      tRobot.timestamp mustEqual 3
      tRobot.dnaId mustEqual 4
      val robot2 = Robot.fromThrift(tRobot)
      robot2.robotId mustEqual 1
      robot2.userId mustEqual 2
      robot2.timestamp mustEqual 3
      robot2.dnaId mustEqual 4
      robot2.payloads("123") mustEqual Some(1)
      robot2.payloads("222") mustEqual Some("xxx")
      robot2.payloads("333") mustEqual None
    }
  }
}
