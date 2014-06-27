/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.data

import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import com.coinport.coinex.metrics._

object MetricsState {
  def apply(tms: TMetricsState): MetricsState = MetricsState(
    Map.empty ++ tms.observers.map(kv => (kv._1 -> MetricsObserver(kv._2)))
  )
}

// MetricsObserver is mutable and need clone when snapshoting
case class MetricsState(
    observers: Map[MarketSide, MetricsObserver] = Map.empty[MarketSide, MetricsObserver]) {

  def pushEvent(side: MarketSide, event: MarketEvent, tick: Long): MetricsState = {
    val observer = observers.getOrElse(side, new MetricsObserver(side))
    observer.pushEvent(event, tick)
    copy(observers = observers + (side -> observer))
  }

  def getMetrics(tick: Long): Metrics = {
    Metrics(observers.map(item =>
      (item._1 -> item._2.copy.pushEvent(null, tick).getMetrics)
    ))
  }

  def snapshot: MetricsState = {
    val newObservers = observers map (item => (item._1 -> item._2.copy))
    copy(observers = newObservers)
  }

  def toThrift: TMetricsState = TMetricsState(observers.map(kv => (kv._1 -> kv._2.toThrift)), null)
}
