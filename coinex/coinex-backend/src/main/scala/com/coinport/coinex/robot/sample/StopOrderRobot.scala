/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.robot.sample

import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import Implicits._

object StopOrderRobot {
  def apply(robotId: Long, userId: Long, timestamp: Long,
    stopPrice: Double, side: MarketSide, order: Order): (Map[String, Option[Any]], Map[String, String]) = {
    val dna = Map(
      "START" -> """
        (robot -> "LISTENING", None)
      """,

      "LISTENING" -> """
        val stopPrice = robot.getPayload[Double]("SP").get
        val side = robot.getPayload[MarketSide]("SIDE").get
        val order = robot.getPayload[Order]("ORDER").get
        metrics match {
          case Some(m) => m.metricsByMarket.get(side) match {
            case Some(mbm) if (mbm.price <= stopPrice) =>
              val action = Some(DoSubmitOrder(side,
                order.copy(userId = robot.userId, robotId = Some(robot.robotId), robotType = Some(%d))))
              (robot -> "DONE", action)
            case _ => (robot -> "LISTENING", None)
          }
          case _ => (robot -> "LISTENING", None)
        }
      """.format(STOP_ORDER_ROBOT_TYPE)
    )

    val payload =
      Map("SP" -> Some(stopPrice),
        "SIDE" -> Some(side),
        "ORDER" -> Some(order))

    (payload, dna)
  }
}
