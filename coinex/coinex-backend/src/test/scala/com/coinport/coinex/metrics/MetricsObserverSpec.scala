/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import org.specs2.mutable._

import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import Currency._
import Direction._
import Implicits._

class MetricsObserverSpec extends Specification {
  "MetricsObserver" should {
    "maintain the ticker info" in {
      val observer = new MetricsObserver((Btc ~> Cny), transactionQueue = new WindowVector[MarketEvent](3))
      observer.pushEvent(create(12.5, 100, 0), 0)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 12.5, Some(12.5), Some(12.5), 100, Some(0.0), Keep)
      observer.pushEvent(create(2.5, 90, 0), 0)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, Some(2.5), Some(12.5), 190, Some(-10 / 12.5), Down)
      observer.pushEvent(null, 3)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, None, None, 0, None, Down)

      observer.pushEvent(create(2.5, 10, 11), 11)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, Some(2.5), Some(2.5), 10, Some(0.0), Keep)
      observer.pushEvent(create(3.5, 8, 12), 12)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 3.5, Some(2.5), Some(3.5), 18, Some(1 / 2.5), Up)
      observer.pushEvent(create(12.5, 7, 13), 13)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 12.5, Some(2.5), Some(12.5), 25, Some(10 / 2.5), Up)
      observer.pushEvent(create(4.5, 1, 14), 14)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 4.5, Some(3.5), Some(12.5), 16, Some(1 / 3.5), Down)
      observer.pushEvent(create(0.5, 12, 35), 35)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 0.5, Some(0.5), Some(0.5), 12, Some(0.0), Down)
    }

    "convert with thirft: TMetricsObserver" in {
      val observer = new MetricsObserver((Btc ~> Cny), transactionQueue = new WindowVector[MarketEvent](3))
      observer.pushEvent(create(12.5, 100, 0), 0)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 12.5, Some(12.5), Some(12.5), 100, Some(0.0), Keep)
      observer.pushEvent(create(2.5, 90, 0), 0)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, Some(2.5), Some(12.5), 190, Some(-10 / 12.5), Down)
      observer.pushEvent(null, 3)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, None, None, 0, None, Down)

      observer.pushEvent(create(2.5, 10, 11), 11)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 2.5, Some(2.5), Some(2.5), 10, Some(0.0), Keep)
      observer.pushEvent(create(3.5, 8, 12), 12)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 3.5, Some(2.5), Some(3.5), 18, Some(1 / 2.5), Up)
      observer.pushEvent(create(12.5, 7, 13), 13)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 12.5, Some(2.5), Some(12.5), 25, Some(10 / 2.5), Up)
      observer.pushEvent(create(4.5, 1, 14), 14)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 4.5, Some(3.5), Some(12.5), 16, Some(1 / 3.5), Down)
      observer.pushEvent(create(0.5, 12, 35), 35)
      observer.getMetrics mustEqual MetricsByMarket(
        MarketSide(Btc, Cny), 0.5, Some(0.5), Some(0.5), 12, Some(0.0), Down)

      observer.toString mustEqual MetricsObserver(observer.toThrift).toString
    }
  }

  private def create(p: Double, v: Long, t: Long) = MarketEvent(Some(p), Some(v), Some(t))
}
