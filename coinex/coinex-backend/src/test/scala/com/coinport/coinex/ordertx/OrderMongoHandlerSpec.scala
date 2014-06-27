/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.ordertx

import com.coinport.coinex.data.Currency.{ Btc, Cny }
import com.coinport.coinex.common.EmbeddedMongoForTestWithBF
import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
//import com.mongodb.casbah.MongoConnection

class OrderMongoHandlerSpec extends EmbeddedMongoForTestWithBF {

  class OrderClass extends OrderMongoHandler {
    val coll = database("OrderMongoHandlerSpec")
    //    val database = MongoConnection("localhost", 27017)("test")
    //    val coll = database("order")
  }

  val market = Btc ~> Cny
  //  step(embeddedMongoStartup())

  "OrderDataStateSpec" should {
    val orderClass = new OrderClass()
    "can save update OrderInfo and can sort count data" in {
      orderClass.coll.drop()
      orderClass.coll.size should be(0)
      var orderInfos = (0 to 3).map(i => OrderInfo(market, Order(i, i, i), 10, 10, OrderStatus.Pending, None))
      orderInfos.foreach(oi => orderClass.addItem(oi, 0))

      orderClass.coll.size should be(4)

      orderClass.coll.drop()
      orderInfos = (0 to 3).map(i => OrderInfo(market, Order(i, i, i), 10, 10, OrderStatus.Pending, None))
      orderInfos.foreach(oi => orderClass.addItem(oi, 20))

      val refund = Refund(reason = RefundReason.HitTakeLimit, amount = 10L)
      orderClass.updateItem(1, 10, 0, 1, market.reverse, 20, Some(refund))

      var q = QueryOrder(oid = Some(1L), cursor = Cursor(0, 2))

      val order_info = orderClass.getItems(q)(0)
      order_info.side should equal(market.reverse)
      order_info.order.userId should be(1)
      order_info.order.id should be(1)
      order_info.inAmount should be(10)
      order_info.outAmount should be(1)
      order_info.status.getValue() should be(1)
      order_info.lastTxTimestamp should be(Some(20))
      order_info.order.refund should be(Some(refund))

      orderInfos = (0 to 10).map(i => OrderInfo(market, Order(i % 3, i, i), 10, 10, OrderStatus.Pending, None))
      orderInfos.foreach(oi => orderClass.addItem(oi, 0))

      q = QueryOrder(uid = Some(1L), cursor = Cursor(0, 10))
      orderClass.countItems(q) should be(4)

      q = QueryOrder(uid = Some(1L), cursor = Cursor(0, 10))
      orderClass.countItems(q) should be(4)
      orderClass.getItems(q).map(_.order.id) should equal(Seq(10, 7, 4, 1))

      orderClass.coll.drop()
      orderInfos = (0 to 3).map(i => OrderInfo(if (i % 2 == 0) market else market.reverse, Order(i, i, i), 10, 10, OrderStatus.Pending, None))
      orderInfos.foreach(oi => orderClass.addItem(oi, 0))

      q = QueryOrder(side = Some(QueryMarketSide(market, true)), cursor = Cursor(0, 10))
      orderClass.getItems(q).map(_.order.id) should equal(Seq(3, 2, 1, 0))
      q = QueryOrder(side = Some(QueryMarketSide(market, false)), cursor = Cursor(0, 10))
      orderClass.getItems(q).map(_.order.id) should equal(Seq(2, 0))
      q = QueryOrder(side = Some(QueryMarketSide(market.reverse, false)), cursor = Cursor(0, 10))
      orderClass.getItems(q).map(_.order.id) should equal(Seq(3, 1))
    }
  }
}
