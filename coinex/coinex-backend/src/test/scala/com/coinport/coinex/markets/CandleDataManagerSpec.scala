/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.markets

import org.specs2.mutable._
import com.coinport.coinex.data._
import Implicits._
import Currency._
import com.coinport.coinex.data.ChartTimeDimension._

class CandleDataManagerSpec extends Specification {
  "ChartDataManagerSpec" should {
    val market = Btc ~> Cny

    "save data from order submitted" in {
      val makerPrevious = Order(userId = 555, id = 1, price = Some(3000 reciprocal), quantity = 3000, takeLimit = None, timestamp = Some(0))
      val makerCurrent = Order(userId = 555, id = 2, price = Some(3000 reciprocal), quantity = 0, takeLimit = None, timestamp = Some(0))
      val takerPrevious = Order(userId = 888, id = 3, price = Some(3000), quantity = 1, timestamp = Some(0))
      val takerCurrent = Order(userId = 888, id = 4, price = Some(3000), quantity = 0, timestamp = Some(0))

      val t = Transaction(1L, 1000000, market.reverse, OrderUpdate(takerPrevious, takerCurrent), OrderUpdate(makerPrevious, makerCurrent))

      val manager = new CandleDataManager(market)

      manager.updateCandleItem(t)

      manager.getCandleItems(OneMinute, 800000, 1200000) mustEqual
        Seq(
          CandleDataItem(13, 0, 0, 0, 0, 0, 0),
          CandleDataItem(14, 0, 0, 0, 0, 0, 0),
          CandleDataItem(15, 0, 0, 0, 0, 0, 0),
          CandleDataItem(16, 1, 3000, 1 / 3000.0, 1 / 3000.0, 1 / 3000.0, 1 / 3000.0),
          CandleDataItem(17, 0, 0, 0, 0, 0, 0),
          CandleDataItem(18, 0, 0, 0, 0, 0, 0),
          CandleDataItem(19, 0, 0, 0, 0, 0, 0),
          CandleDataItem(20, 0, 0, 0, 0, 0, 0)
        )
    }

    "can fill empty candle by transaction" in {
      val makerPrevious = Order(userId = 555, id = 1, price = Some(3000 reciprocal), quantity = 3000, takeLimit = None, timestamp = Some(0))
      val makerCurrent = Order(userId = 555, id = 2, price = Some(3000 reciprocal), quantity = 0, takeLimit = None, timestamp = Some(0))
      val takerPrevious = Order(userId = 888, id = 3, price = Some(3000), quantity = 1, timestamp = Some(0))
      val takerCurrent = Order(userId = 888, id = 4, price = Some(3000), quantity = 0, timestamp = Some(0))

      val makerPrevious2 = Order(userId = 555, id = 1, price = Some(2000 reciprocal), quantity = 4000, takeLimit = None, timestamp = Some(0))
      val makerCurrent2 = Order(userId = 555, id = 2, price = Some(2000 reciprocal), quantity = 0, takeLimit = None, timestamp = Some(0))
      val takerPrevious2 = Order(userId = 888, id = 3, price = Some(2000), quantity = 2, timestamp = Some(0))
      val takerCurrent2 = Order(userId = 888, id = 4, price = Some(2000), quantity = 0, timestamp = Some(0))

      val manager = new CandleDataManager(market)
      val c1 = System.currentTimeMillis()
      val t1 = Transaction(1L, c1, market.reverse, OrderUpdate(takerPrevious, takerCurrent), OrderUpdate(makerPrevious, makerCurrent))
      val t2 = Transaction(1L, c1 + 30 * 60 * 1000, market.reverse, OrderUpdate(takerPrevious2, takerCurrent2), OrderUpdate(makerPrevious2, makerCurrent2))

      manager.updateCandleItem(t1)
      manager.updateCandleItem(t2)

      val rv = manager.query(OneMinute, c1, c1 + 30 * 60 * 1000 * 2)
      rv.size mustEqual 61
      (0 until rv.size).foreach { i =>
        if (i < 30) rv.apply(i).open mustEqual (1.0 / 3000)
        else rv.apply(i).close mustEqual (1.0 / 2000)
      }

      val rv2 = manager.query(OneDay, c1, c1 + 30 * 60 * 1000 * 2)
      rv2.size mustEqual 1
      val item = rv2.apply(0)
      item.open mustEqual (1.0 / 3000)
      item.close mustEqual (1.0 / 2000)
    }

    "can fill empty candle by query" in {
      val manager = new CandleDataManager(market)
      val makerPrevious = Order(userId = 555, id = 1, price = Some(3000 reciprocal), quantity = 3000, takeLimit = None, timestamp = Some(0))
      val makerCurrent = Order(userId = 555, id = 2, price = Some(3000 reciprocal), quantity = 0, takeLimit = None, timestamp = Some(0))
      val takerPrevious = Order(userId = 888, id = 3, price = Some(3000), quantity = 1, timestamp = Some(0))
      val takerCurrent = Order(userId = 888, id = 4, price = Some(3000), quantity = 0, timestamp = Some(0))

      val makerPrevious2 = Order(userId = 555, id = 1, price = Some(2000 reciprocal), quantity = 4000, takeLimit = None, timestamp = Some(0))
      val makerCurrent2 = Order(userId = 555, id = 2, price = Some(2000 reciprocal), quantity = 0, takeLimit = None, timestamp = Some(0))
      val takerPrevious2 = Order(userId = 888, id = 3, price = Some(2000), quantity = 2, timestamp = Some(0))
      val takerCurrent2 = Order(userId = 888, id = 4, price = Some(2000), quantity = 0, timestamp = Some(0))

      val c1 = System.currentTimeMillis()
      val t1 = Transaction(1L, c1, market.reverse, OrderUpdate(takerPrevious, takerCurrent), OrderUpdate(makerPrevious, makerCurrent))
      val t2 = Transaction(1L, c1 + 30 * 60 * 1000, market.reverse, OrderUpdate(takerPrevious2, takerCurrent2), OrderUpdate(makerPrevious2, makerCurrent2))

      manager.query(OneMinute, c1, c1 + 30 * 60 * 1000 * 2) mustEqual
        (c1 / manager.getTimeSkip(OneMinute) to (c1 + 30 * 60 * 1000 * 2) / manager.getTimeSkip(OneMinute))
        .map(t => CandleDataItem(t, 0, 0, 0, 0, 0, 0))

      manager.candleMap.get(OneMinute).get.size mustEqual 0

      manager.updateCandleItem(t1)
      manager.candleMap.get(OneMinute).get.size mustEqual 1
      manager.updateCandleItem(t2)
      manager.candleMap.get(OneMinute).get.size mustEqual 31

      val rv = manager.query(OneMinute, c1, c1 + 30 * 60 * 1000 * 2)
      manager.candleMap.get(OneMinute).get.size mustEqual 60

      (0 until rv.size).foreach {
        i =>
          if (i < 30) rv.apply(i).open mustEqual (1.0 / 3000)
          else if (i >= 30) rv.apply(i).open mustEqual (1.0 / 2000)
      }
      rv.size mustEqual 61
    }
  }
}
