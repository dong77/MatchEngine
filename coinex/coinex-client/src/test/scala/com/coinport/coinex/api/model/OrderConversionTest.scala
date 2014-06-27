/**
 * Copyright (C) 2014 Coinport Inc.
 */
package com.coinport.coinex.api.model

import org.specs2.mutable._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data._
import com.coinport.coinex.api.model.Operations._
import scala.Some
import scala.Some

class OrderConversionTest extends Specification {
  "order conversions" should {
    "user order conversion" in {
      var order: DoSubmitOrder = null
      var command: DoSubmitOrder = null
      val uid = 123L

      // buy orders

      // buy 2 BTC at 4100 CNY/BTC, spending 8000 CNY
      order = UserOrder(uid.toString, Buy, Btc, Cny, Some(4100.0), Some(2), Some(8200)).toDoSubmitOrder
      // sell 820000 CNY2 at price of 1000 / 410000 CNY2 per MBTC for 2000 MBTC
      command = DoSubmitOrder(Cny ~> Btc, Order(uid, 0L, 8200.0.internalValue(Cny), Some(4.1 reciprocal), Some(2.internalValue(Btc))))
      order mustEqual command

      // buy 2 BTC at 4000 CNY/BTC, same to above
      order = UserOrder(uid.toString, Buy, Btc, Cny, Some(4000), Some(2), None).toDoSubmitOrder
      // sell 800000 CNY2 at price of 1000 / 400000 CNY2 per MBTC for 2000 MBTC
      command = DoSubmitOrder(Cny ~> Btc, Order(uid, 0L, 8000.0.internalValue(Cny), Some(4.0 reciprocal), Some(2.internalValue(Btc))))
      order mustEqual command

      // market order: buy some BTC at any price, spending 8000 CNY
      order = UserOrder(uid.toString, Buy, Btc, Cny, None, None, Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at any price for some MBTC
      command = DoSubmitOrder(Cny ~> Btc, Order(uid, 0L, 8000.0.internalValue(Cny), None, None))
      order mustEqual command

      // limited market order: buy some BTC at 4000 CNY/BTC, spending 8000 CNY
      order = UserOrder(uid.toString, Buy, Btc, Cny, Some(4000), None, Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at 1000 / 400000 CNY2 per MBTC for some MBTC
      command = DoSubmitOrder(Cny ~> Btc, Order(uid, 0L, 8000.0.internalValue(Cny), Some(4.0 reciprocal), None))
      order mustEqual command
      // limited market order: buy 2 BTC, at any price, spending 8000 CNY
      order = UserOrder(uid.toString, Buy, Btc, Cny, None, Some(2), Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at any price for 2000 MBTC
      command = DoSubmitOrder(Cny ~> Btc, Order(uid, 0L, 8000.0.internalValue(Cny), None, Some(2.internalValue(Btc))))
      order mustEqual command

      // sell orders

      // sell 2 BTC at 5000 CNY/BTC, for 10000 CNY
      order = UserOrder(uid.toString, Sell, Btc, Cny, Some(5000), Some(2), Some(10000)).toDoSubmitOrder
      // sell 2000 MBTC at 5000 * 100 / 1000 CNY2/MBTC, for 10000 * 100 CNY2
      command = DoSubmitOrder(Btc ~> Cny, Order(uid, 0L, 2.internalValue(Btc), Some(5.0), Some(10000.internalValue(Cny))))
      order mustEqual command

      // sell 2 BTC at 5000 CNY/BTC
      order = UserOrder(uid.toString, Sell, Btc, Cny, Some(5000), Some(2), None).toDoSubmitOrder
      // sell 2000 MBTC at 500 CNY2/MBTC
      command = DoSubmitOrder(Btc ~> Cny, Order(uid, 0L, 2.internalValue(Btc), Some(5.0), None))
      order mustEqual command

      // market order: sell 2 BTC at any price
      order = UserOrder(uid.toString, Sell, Btc, Cny, None, Some(2), None).toDoSubmitOrder
      // sell 2000 MBTC at any price
      command = DoSubmitOrder(Btc ~> Cny, Order(uid, 0L, 2.internalValue(Btc), None, None))
      order mustEqual command

      // limit market order: sell 2 BTC at any price, for 10000 CNY
      order = UserOrder(uid.toString, Sell, Btc, Cny, None, Some(2), Some(10000)).toDoSubmitOrder
      // sell 2000 BTC at any price, for 1000000 CNY2
      command = DoSubmitOrder(Btc ~> Cny, Order(uid, 0L, 2.internalValue(Btc), None, Some(10000.internalValue(Cny))))
      order mustEqual command

      // sell some BTC at 5000 CNY/BTC, for 10000 CNY
      order = UserOrder(uid.toString, Sell, Btc, Cny, Some(5000), None, Some(10000)).toDoSubmitOrder
      // sell some BTC at 500 CNY2/MBTC, for 1000000 CNY2
      command = DoSubmitOrder(Btc ~> Cny, Order(uid, 0L, (10000 / 5000).internalValue(Btc), Some(5.0), Some(10000.internalValue(Cny))))
      order mustEqual command

      // convert back
      var userOrder = UserOrder(uid.toString, Sell, Btc, Cny, Some(1234), Some(12), None)
      userOrder mustEqual UserOrder.fromOrderInfo(OrderInfo(Btc ~> Cny, userOrder.toDoSubmitOrder.order, 0, 0, OrderStatus.Pending))

      userOrder = UserOrder(uid.toString, Buy, Btc, Cny, Some(1234), Some(12), Some(1234 * 12))
      userOrder mustEqual UserOrder.fromOrderInfo(OrderInfo(Cny ~> Btc, userOrder.toDoSubmitOrder.order, 0, 0, OrderStatus.Pending))

      // inAmount / outAmount
      var backOrder = OrderInfo(
        Btc ~> Cny,
        Order(uid, 0L, 2.0.internalValue(Btc), Some(5.0), None),
        2.0.internalValue(Btc), // out
        (5000 * 2).internalValue(Cny), // in
        OrderStatus.FullyExecuted)
      var frontOrder = UserOrder(
        uid.toString,
        operation = Sell,
        subject = Btc,
        currency = Cny,
        price = Some(5000.0),
        amount = Some(2),
        total = None,
        finishedQuantity = 2.0,
        finishedAmount = 10000.0,
        status = OrderStatus.FullyExecuted.getValue
      )

      UserOrder.fromOrderInfo(backOrder) mustEqual frontOrder

      backOrder = OrderInfo(
        Btc ~> Cny,
        Order(uid, 0L, 3.0.internalValue(Btc), Some(5.0), None),
        1.0.internalValue(Btc), // out
        5000.internalValue(Cny), // in
        OrderStatus.PartiallyExecuted)
      frontOrder = UserOrder(
        uid.toString,
        operation = Sell,
        subject = Btc,
        currency = Cny,
        price = Some(5000.0),
        amount = Some(3),
        total = None,
        finishedQuantity = 1.0,
        finishedAmount = 5000.0,
        status = OrderStatus.PartiallyExecuted.getValue
      )

      UserOrder.fromOrderInfo(backOrder) mustEqual frontOrder

      // buy 3 BTC at 4000 CNY/BTC
      backOrder = OrderInfo(
        Cny ~> Btc,
        Order(uid, 0L, (3 * 4000).internalValue(Cny), Some(4.0 reciprocal), Some(3.internalValue(Btc))),
        (3 * 4000).internalValue(Cny), // out
        3.internalValue(Btc), // in
        OrderStatus.FullyExecuted)
      frontOrder = UserOrder(
        uid.toString,
        operation = Buy,
        subject = Btc,
        currency = Cny,
        price = Some(4000.0),
        amount = Some(3),
        total = Some(3 * 4000),
        finishedQuantity = 3.0,
        finishedAmount = 12000.0,
        status = OrderStatus.FullyExecuted.getValue
      )

      UserOrder.fromOrderInfo(backOrder) mustEqual frontOrder

      // buy 3 BTC at 4000 CNY/BTC
      backOrder = OrderInfo(
        Cny ~> Btc,
        Order(uid, 0L, (3 * 4000).internalValue(Cny), Some(4.0 reciprocal), Some(3.internalValue(Btc))),
        3000.internalValue(Cny), // outAmount, spent 3000 CNY
        1.internalValue(Btc), // inAmount, bought 1 BTC
        OrderStatus.PartiallyExecuted)
      frontOrder = UserOrder(
        uid.toString,
        operation = Buy,
        subject = Btc,
        currency = Cny,
        price = Some(4000.0),
        amount = Some(3),
        total = Some(4000 * 3), // total amount
        finishedQuantity = 1.0, // bought 1 BTC
        finishedAmount = 3000.0, // spent 3000 CNY
        status = OrderStatus.PartiallyExecuted.getValue
      )

      UserOrder.fromOrderInfo(backOrder) mustEqual frontOrder
    }
    "very small number" in {
      val order = UserOrder("123", Buy, Doge, Btc, Some(0.0000001), Some(100), None)
      order.toDoSubmitOrder mustEqual DoSubmitOrder(Btc ~> Doge, Order(123L, 0L, 0.00001.internalValue(Btc), Some(0.0000001 reciprocal), Some(100.internalValue(Doge))))
    }
  }
}
