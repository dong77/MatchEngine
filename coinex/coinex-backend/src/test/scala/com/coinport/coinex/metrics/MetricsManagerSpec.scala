/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import org.specs2.mutable._

import com.coinport.coinex.data._
import Currency._
import Direction._
import Implicits._

class MetricsManagerSpec extends Specification {
  "MetricsManager" should {
    "the max should be the max value (bug fix)" in {
      val side = (Btc ~> Cny)
      val manager = new MetricsManager()
      manager.update(side, RDouble(0.2, false).reciprocal, 10, 2, 1)
      manager.update(side, RDouble(0.4, false).reciprocal, 10, 2, 2)
      manager.update(side, RDouble(0.1, false).reciprocal, 10, 2, 3)
      manager.update(side, RDouble(0.3, false).reciprocal, 10, 2, 4)
      val metrics = Metrics(Map(MarketSide(Btc, Cny) -> MetricsByMarket(MarketSide(Btc, Cny), 3.3333333333333335, Some(2.5), Some(10.0), 40, Some(-0.3333333333333333), Down), MarketSide(Cny, Btc) -> MetricsByMarket(MarketSide(Cny, Btc), 0.3, Some(0.1), Some(0.4), 8, Some(0.4999999999999999), Up)))
      manager.getMetrics(4) mustEqual metrics
      val newManager = new MetricsManager()
      newManager.loadSnapshot(manager.getSnapshot)
      newManager.getMetrics(4) mustEqual metrics
      newManager.update(side, RDouble(0.5, false).reciprocal, 10, 2, 5)
      newManager.getMetrics(4).metricsByMarket(side).high mustEqual Some(10)
      newManager.getMetrics(10000000000L) mustEqual Metrics(Map(MarketSide(Btc, Cny) -> MetricsByMarket(MarketSide(Btc, Cny), 2.0, None, None, 0, None, Down), MarketSide(Cny, Btc) -> MetricsByMarket(MarketSide(Cny, Btc), 0.5, None, None, 0, None, Up)))
    }
  }
}
