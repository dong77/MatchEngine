/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import scala.collection.mutable.ArrayBuffer

import com.coinport.coinex.common.Constants._
import com.coinport.coinex.data._
import Direction._

object MetricsObserver {
  def apply(tmo: TMetricsObserver): MetricsObserver = {
    val ttq = tmo.transactionQueue
    val tMin = tmo.minMaintainer
    val tMax = tmo.maxMaintainer
    val tPre = tmo.preMaintainer
    new MetricsObserver(
      tmo.side,
      new WindowVector[MarketEvent](ttq.range, ttq.elems.map(e => (e, e.timestamp.get)).to[ArrayBuffer]),
      new StackQueue[Double](tMin.elems.to[ArrayBuffer], ascending),
      new StackQueue[Double](tMax.elems.to[ArrayBuffer], descending),
      new StackQueue[Double](tPre.elems.to[ArrayBuffer], ((l, r) => true)),
      tmo.price, tmo.lastPrice, tmo.volumeMaintainer
    )
  }
}

class MetricsObserver(
    side: MarketSide,
    transactionQueue: WindowVector[MarketEvent] = new WindowVector[MarketEvent](_24_HOURS),
    minMaintainer: StackQueue[Double] = new StackQueue[Double](ascending),
    maxMaintainer: StackQueue[Double] = new StackQueue[Double](descending),
    preMaintainer: StackQueue[Double] = new StackQueue[Double]((l, r) => true),
    var price: Option[Double] = None,
    var lastPrice: Option[Double] = None,
    var volumeMaintainer: Long = 0L) {

  def pushEvent(event: MarketEvent, tick: Long) = {
    transactionQueue.addAtTick(event, tick) foreach { e =>
      e match {
        case null => None
        case MarketEvent(Some(p), Some(v), _) =>
          minMaintainer.dequeue(p)
          maxMaintainer.dequeue(p)
          preMaintainer.dequeue(p)
          volumeMaintainer -= v
        case _ => None
      }
    }
    event match {
      case null => None
      case MarketEvent(Some(p), Some(v), _) =>
        minMaintainer.push(p)
        maxMaintainer.push(p)
        preMaintainer.push(p)
        lastPrice = price
        price = Some(p)
        volumeMaintainer += v
      case _ => None
    }
    this
  }

  def getMetrics: MetricsByMarket = {
    val gain: Option[Double] = (preMaintainer.front, price) match {
      case (Some(prp), Some(p)) => Some((p - prp) / prp)
      case _ => None
    }
    val direction = (lastPrice, price) match {
      case (Some(lp), Some(p)) => if (lp > p) Down else if (lp < p) Up else Keep
      case _ => Keep
    }
    MetricsByMarket(
      side, price.getOrElse(0.0), minMaintainer.front, maxMaintainer.front, volumeMaintainer, gain, direction)
  }

  def copy = new MetricsObserver(side, transactionQueue.copy, minMaintainer.copy, maxMaintainer.copy,
    preMaintainer.copy, price, lastPrice, volumeMaintainer)

  def toThrift: TMetricsObserver = {
    val tTransactionQueue = TWindowVector(transactionQueue.range, transactionQueue.toList.map(_._1))
    val tMinMaintainer = TStackQueue(minMaintainer.toList)
    val tMaxMaintainer = TStackQueue(maxMaintainer.toList)
    val tPreMaintainer = TStackQueue(preMaintainer.toList)
    TMetricsObserver(side = side, tTransactionQueue, tMinMaintainer, tMaxMaintainer, tPreMaintainer, price = price,
      lastPrice = lastPrice, volumeMaintainer = volumeMaintainer)
  }

  override def toString() = """side: %s; transactionQueue: %s; minMaintainer: %s; maxMaintainer: %s;
    | preMaintainer: %s; price: %s; lastPrice: %s; volumeMaintainer: %s""".stripMargin.format(
    side, transactionQueue, minMaintainer, maxMaintainer, preMaintainer, price, lastPrice, volumeMaintainer)
}
