/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 *
 * *IMPORTANT* For buy side, the stopPercentage need re-compute:
 *   new = old / (1 + old)
 * for example:
 * File a buy btc order if price increase 10%. This will change to:
 * File a sell btc order if reverse price (1/originPrice) decrease (10% / (1 + 10%))
 */

package com.coinport.coinex.robot.sample

import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import Implicits._

object TrailingStopOrderRobot {
  def apply(robotId: Long, userId: Long, timestamp: Long, basePrice: Double,
    stopPercentage: Double, side: MarketSide, order: Order): (Map[String, Option[Any]], Map[String, String]) = {
    val dna = Map(
      "START" -> """
        (robot -> "LISTENING", None)
      """,

      "LISTENING" -> """
        val stopPercentage = robot.getPayload[Double]("SP").get
        val side = robot.getPayload[MarketSide]("SIDE").get
        val order = robot.getPayload[Order]("ORDER").get
        val basePrice = robot.getPayload[Double]("BP").getOrElse(-1).asInstanceOf[Double]
        metrics match {
          case Some(m) => m.metricsByMarket.get(side) match {
            case Some(mbm) =>
              if (mbm.price <= stopPercentage * basePrice) {
                val action = Some(DoSubmitOrder(side,
                  order.copy(userId = robot.userId, robotId = Some(robot.robotId), robotType = Some(%d))))
                (robot -> "DONE", action)
              } else {
                val r = if (mbm.price <= basePrice) robot else robot.setPayload("BP", Some(mbm.price))
                (r -> "LISTENING", None)
              }
            case _ =>
              (robot -> "LISTENING", None)
          }
          case _ => (robot -> "LISTENING", None)
        }
      """.format(TRAILING_STOP_ORDER_ROBOT_TYPE)
    )

    val payload = Map("BP" -> Some(basePrice),
      "SP" -> Some(1 - stopPercentage),
      "SIDE" -> Some(side),
      "ORDER" -> Some(order))

    (payload, dna)
  }
}
