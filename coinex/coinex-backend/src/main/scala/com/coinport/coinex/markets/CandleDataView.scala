/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.markets

import com.coinport.coinex.data.ChartTimeDimension._
import akka.event.LoggingReceive
import akka.persistence.Persistent
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import com.coinport.coinex.common._
import scala.collection.mutable.Map
import scala.concurrent.duration._
import Implicits._

class CandleDataView(market: MarketSide) extends ExtendedView {
  override def processorId = MARKET_UPDATE_PROCESSOR <<
  override val viewId = CANDLE_DATA_VIEW << market

  val manager = new CandleDataManager(market)

  def receive = LoggingReceive {
    case Persistent(OrderSubmitted(orderInfo, txs), _) if orderInfo.side == market || orderInfo.side == market.reverse =>
      txs.foreach(tx => manager.updateCandleItem(tx))

    case QueryCandleData(side, dimension, from, to) if side == market || side == market.reverse =>
      val items = manager.query(dimension, from, to)

      sender ! QueryCandleDataResult(CandleData(items, side))
  }
}

class CandleDataManager(marketSide: MarketSide) extends Manager[TCandleDataState] {

  var candleMap = Map.empty[ChartTimeDimension, Map[Long, CandleDataItem]]
  ChartTimeDimension.list.foreach(d => candleMap.put(d, Map.empty[Long, CandleDataItem]))
  var firstTimestamp = 0L

  override def getSnapshot = TCandleDataState(candleMap, firstTimestamp)

  override def loadSnapshot(snapshot: TCandleDataState) {
    candleMap = candleMap.empty ++ snapshot.candleMap.map {
      x =>
        x._1 -> (Map.empty[Long, CandleDataItem] ++ x._2)
    }

    firstTimestamp = snapshot.firstTimestmap
  }

  def query(d: ChartTimeDimension, from: Long, to: Long) = {
    val start = Math.min(from, to)
    val stop = Math.max(from, to)

    fillEmptyCandleByTimeDimension(stop, d)
    getCandleItems(d, start, stop).toSeq
  }

  def updateCandleItem(t: Transaction) {
    val tout = t.takerUpdate.previous.quantity - t.takerUpdate.current.quantity
    val tin = t.makerUpdate.previous.quantity - t.makerUpdate.current.quantity
    val mprice = t.makerUpdate.current.price.get.value
    val timestamp = t.timestamp
    val (price, out, in) = if (t.side == marketSide) (mprice.reciprocal.value, tout, tin) else (mprice, tin, tout)

    if (firstTimestamp == 0L) firstTimestamp = t.timestamp

    ChartTimeDimension.list.foreach { d =>
      val skipper = getTimeSkip(d)
      val key = timestamp / skipper
      val itemMap = candleMap.get(d).get
      val item = itemMap.get(key) match {
        case Some(item) =>
          CandleDataItem(key, item.inAoumt + in, item.outAoumt + out,
            item.open, price, Math.min(item.low, price), Math.max(item.high, price))
        case None =>
          fillEmptyCandleByTimeDimension(timestamp, d)
          CandleDataItem(key, in, out, price, price, price, price)
      }
      itemMap.put(key, item)
    }
  }

  def getCandleItems(dimension: ChartTimeDimension, from: Long, to: Long) = {
    val timeSkipper = getTimeSkip(dimension)
    val itemMap = candleMap.get(dimension).get

    val tickBegin = from / timeSkipper
    val tickEnd = to / timeSkipper
    val items = (tickBegin to tickEnd - 1).map(t => (t -> itemMap.get(t))).map {
      case (time, item) =>
        item.getOrElse(CandleDataItem(time, 0, 0, 0.0, 0.0, 0.0, 0.0))
    }
    //if the latest candle does not exist, then will fake one
    val lastItem = itemMap.get(tickEnd) match {
      case Some(item) => item
      case None => itemMap.get(tickEnd - 1) match {
        case Some(item) => CandleDataItem(tickEnd, 0, 0, item.close, item.close, item.close, item.close)
        case None => CandleDataItem(tickEnd, 0, 0, 0.0, 0.0, 0.0, 0.0)
      }
    }

    items :+ lastItem
  }

  private def fillEmptyCandleByTimeDimension(timestamp: Long, d: ChartTimeDimension) = {
    var seq = Seq.empty[Long]
    val itemMap = candleMap.get(d).get
    var key = (timestamp / getTimeSkip(d) - 1)
    val lowerLimit = firstTimestamp / getTimeSkip(d)

    if (itemMap.nonEmpty) {
      var flag = true
      var price = 0.0

      while (flag && key >= lowerLimit) {
        itemMap.get(key) match {
          case Some(item) =>
            price = item.close
            flag = false
          case None =>
            seq = seq.+:(key)
            key -= 1
        }
      }

      seq.foreach(k => itemMap.put(k, CandleDataItem(k, 0, 0, price, price, price, price)))
    }
  }

  def getTimeSkip(dimension: ChartTimeDimension) = {
    val duration = dimension match {
      case OneMinute => 1 minute
      case ThreeMinutes => 3 minutes
      case FiveMinutes => 5 minutes
      case FifteenMinutes => 15 minutes
      case ThirtyMinutes => 30 minutes
      case OneHour => 1 hour
      case TwoHours => 2 hours
      case FourHours => 4 hours
      case SixHours => 6 hours
      case TwelveHours => 12 hours
      case OneDay => 1 day
      case ThreeDays => 3 days
      case OneWeek => 7 days
    }
    duration.toMillis
  }
}
