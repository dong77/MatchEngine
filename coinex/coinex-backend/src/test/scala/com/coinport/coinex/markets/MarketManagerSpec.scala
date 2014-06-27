/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.markets

import org.specs2.mutable._
import scala.collection.immutable.SortedSet

import com.coinport.coinex.data._
// import com.coinport.coinex.debug.Debugger
import Implicits._
import Currency._
import OrderStatus._
import RefundReason._

class MarketManagerSpec extends Specification {
  val takerSide = Btc ~> Cny
  val makerSide = takerSide.reverse

  import MarketManager._

  "MarketManager" should {

    "very low sell order should take all much higher buy order" in {
      val manager = new MarketManager(Btc ~> Cny)
      val buySide = Cny ~> Btc
      val sellSide = Btc ~> Cny

      manager.addOrderToMarket(buySide, Order(91990591289398244L, 10000000124L, 8999, Some(1.3332077064322006E-4), None, Some(1397829420604L), None, Some(91990591289398244L), None, 10, None))
      manager.addOrderToMarket(buySide, Order(877800447188483646L, 10000000129L, 78351, Some(1.4039210295437187E-4), None, Some(1397829425596L), None, Some(877800447188483646L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(-245561917658914311L, 10000000101L, 13190, Some(1.4403404685562162E-4), None, Some(1397829400476L), None, Some(-245561917658914311L), None, 9, None))
      manager.addOrderToMarket(buySide, Order(877800447188483646L, 10000000111L, 86187, Some(1.6011661328680426E-4), None, Some(1397829410477L), None, Some(877800447188483646L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(5742988204510593740L, 10000000103L, 75676, Some(1.625338326902886E-4), None, Some(1397829400560L), None, Some(5742988204510593740L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(5742988204510593740L, 10000000125L, 78351, Some(1.6336535616508726E-4), None, Some(1397829425430L), None, Some(5742988204510593740L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(877800447188483646L, 10000000106L, 69428, Some(1.685198348210773E-4), None, Some(1397829405477L), None, Some(877800447188483646L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(5742988204510593740L, 10000000083L, 58270, Some(1.698959923591867E-4), None, Some(1397829380583L), None, Some(5742988204510593740L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(5742988204510593740L, 10000000108L, 69428, Some(1.7140051575818973E-4), None, Some(1397829405560L), None, Some(5742988204510593740L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(-6771488127296557565L, 10000000115L, 69428, Some(1.7716187763241458E-4), None, Some(1397829415462L), None, Some(-6771488127296557565L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(91990591289398244L, 10000000077L, 57694, Some(2.131937285937552E-4), None, Some(1397829375607L), None, Some(91990591289398244L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(91990591289398244L, 10000000105L, 69428, Some(2.1605107028343243E-4), None, Some(1397829405436L), None, Some(91990591289398244L), None, 0, None))
      manager.addOrderToMarket(buySide, Order(91990591289398244L, 10000000096L, 72304, Some(2.1713760484859542E-4), None, Some(1397829395484L), None, Some(91990591289398244L), None, 0, None))

      // println(Debugger.prettyOutput(sellSide, manager.getSnapshot))
      val os = manager.addOrderToMarket(sellSide, Order(7410102373723916141L, 0, 10000, Some(10.0), None, Some(1397829427785L), None, None, None, 0, None))

      // println(Debugger.prettyOutput(sellSide, manager.getSnapshot))
      // println(Debugger.prettyOutput(manager.headSide, os))
      manager.getSnapshot mustEqual TMarketState(Map(MarketSide(Cny, Btc) -> List(), MarketSide(Btc, Cny) -> List(
        Order(7410102373723916141L, 0, 9865, Some(10.0), None, Some(1397829427785L), None, None, None, 766228, None))),
        Map(0L -> Order(7410102373723916141L, 0, 9865, Some(10.0), None, Some(1397829427785L), None, None, None, 766228, None)), None, RedeliverFilters(Map()))
    }

    "take limit can't block the current transaction" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 1, id = 1, price = Some(2000 reciprocal), quantity = 20000)
      val maker2 = Order(userId = 2, id = 2, price = Some(5000 reciprocal), quantity = 50000)
      val taker = Order(userId = 3, id = 3, price = Some(2000), quantity = 1, takeLimit = Some(2000))

      manager.addOrderToMarket(makerSide, maker1)
      manager.addOrderToMarket(makerSide, maker2)

      manager.addOrderToMarket(takerSide, taker) mustEqual OrderSubmitted(
        OrderInfo(MarketSide(Btc, Cny),
          Order(3, 3, 1, Some(2000.0), Some(2000), None, None, None, None, 0, None), 1, 5000, FullyExecuted, Some(0)),
        List(Transaction(3001, 0, MarketSide(Btc, Cny),
          OrderUpdate(
            Order(3, 3, 1, Some(2000.0), Some(2000), None, None, None, None, 0, None),
            Order(3, 3, 0, Some(2000.0), Some(0), None, None, None, None, 5000, None)),
          OrderUpdate(
            Order(2, 2, 50000, Some(5000 reciprocal), None, None, None, None, None, 0, None),
            Order(2, 2, 45000, Some(5000 reciprocal), None, None, None, None, None, 1, None)), None)))
    }

    "refund first dust taker" in {
      val manager = new MarketManager(Btc ~> Cny)
      val taker = Order(userId = 1, id = 1, price = Some(2000 reciprocal), quantity = 200)

      manager.addOrderToMarket(makerSide, taker) mustEqual OrderSubmitted(OrderInfo(MarketSide(Cny, Btc),
        Order(1, 1, 200, Some(2000 reciprocal), None, None, None, None, None, 0, Some(Refund(Dust, 200))), 0, 0, FullyExecuted, None), List())
    }

    "change the last taker order in tx" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 0, id = 0, price = Some(2000), quantity = 1)
      val taker = Order(userId = 1, id = 1, price = Some(2000 reciprocal), quantity = 2200)

      manager.addOrderToMarket(makerSide, maker)

      manager.addOrderToMarket(takerSide, taker) mustEqual OrderSubmitted(
        OrderInfo(MarketSide(Btc, Cny),
          Order(1, 1, 2200, Some(2000 reciprocal), None, None, None, None, None, 0, None), 2000, 1, FullyExecuted, Some(0)),
        List(Transaction(1001, 0, MarketSide(Btc, Cny),
          OrderUpdate(
            Order(1, 1, 2200, Some(2000 reciprocal), None, None, None, None, None, 0, None),
            Order(1, 1, 200, Some(2000 reciprocal), None, None, None, None, None, 1, Some(Refund(Dust, 200)))),
          OrderUpdate(
            Order(0, 0, 1, Some(2000.0), None, None, None, None, None, 0, None),
            Order(0, 0, 0, Some(2000.0), None, None, None, None, None, 2000, None)), None)))
    }

    "fix the dust bug from robot test" in {
      val manager = new MarketManager(Btc ~> Cny)

      manager.addOrderToMarket(makerSide, Order(12345, 32, 21930, Some(1.1499999999999999E-4), None, Some(1397457555749L), None, Some(12345), None, 0))
      manager.addOrderToMarket(takerSide, Order(456789, 33, 8, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 0)) mustEqual
        OrderSubmitted(
          OrderInfo(MarketSide(Btc, Cny), Order(456789, 33, 8, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 0), 2, 17391, PartiallyExecuted, Some(1397457555805L)),
          List(
            Transaction(33001, 1397457555805L, MarketSide(Btc, Cny),
              OrderUpdate(
                Order(456789, 33, 8, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 0),
                Order(456789, 33, 6, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 17391)),
              OrderUpdate(
                Order(12345, 32, 21930, Some(1.1499999999999999E-4), None, Some(1397457555749L), None, Some(12345), None, 0),
                Order(12345, 32, 4539, Some(1.1499999999999999E-4), None, Some(1397457555749L), None, Some(12345), None, 2, Some(Refund(Dust, 4539)))), None)))

      manager.orderMap mustEqual Map(33 -> Order(456789, 33, 6, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 17391))
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet(Order(456789, 33, 6, Some(4920.0), None, Some(1397457555805L), None, Some(456789), None, 17391))
    }

    "match limit-price order market-price orders can't exists in the market" in {
      val manager = new MarketManager(Btc ~> Cny)
      val mpo1 = Order(userId = 1, id = 1, price = None, quantity = 30000) // higher priority
      val mpo2 = Order(userId = 2, id = 2, price = None, quantity = 60000)
      val lpo1 = Order(userId = 3, id = 3, price = Some(RDouble(5000, true)), quantity = 10000) // higher priority
      val lpo2 = Order(userId = 4, id = 4, price = Some(RDouble(4000, true)), quantity = 40000)
      val taker = Order(userId = 5, id = 5, price = Some(2000), quantity = 100)

      manager.addOrderToMarket(makerSide, mpo1)
      manager.addOrderToMarket(makerSide, mpo2)
      manager.addOrderToMarket(makerSide, lpo1)
      manager.addOrderToMarket(makerSide, lpo2)

      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedLpo1 = lpo1.copy(quantity = 0, inAmount = 2) // buy 2
      val updatedLpo2 = lpo2.copy(quantity = 0, inAmount = 10) // buy 10
      val updatedMpo1 = mpo1.copy(quantity = 0) // buy 15
      val updatedMpo2 = mpo2.copy(quantity = 0) // buy 30
      val updatedTaker = taker.copy(quantity = 100 - 57)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 12, 50000, PartiallyExecuted, Some(0)),
        Seq(
          Transaction(5001, 0, takerSide, taker --> taker.copy(quantity = 98, inAmount = 10000), lpo1 --> updatedLpo1),
          Transaction(5002, 0, takerSide,
            taker.copy(quantity = 98, inAmount = 10000) --> taker.copy(quantity = 88, inAmount = 50000),
            lpo2 --> updatedLpo2)))
    }
  }

  "MarketManager" should {
    "match limit-price order against existing limit-price order with take-limit and update take-limit" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 666, id = 1, price = Some(4500 reciprocal), quantity = 45099, takeLimit = Some(11))
      val taker = Order(userId = 888, id = 2, price = Some(4000), quantity = 10)

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker = maker.copy(quantity = 99, takeLimit = Some(1), inAmount = 10)
      val updatedTaker = taker.copy(quantity = taker.quantity - 10, inAmount = 45000)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 10, 45000, FullyExecuted, Some(0)),
        Seq(Transaction(2001, 0, takerSide, taker --> updatedTaker, maker --> updatedMaker.copy(refund = Some(Refund(Dust, 99))))))

      manager.orderMap mustEqual Map()
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "match limit-price order with as many existing limit-price order, market should refund over-charged quantity" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 666, id = 1, price = Some(4500 reciprocal), quantity = 10000, takeLimit = Some(1))
      val maker2 = Order(userId = 777, id = 2, price = Some(5000 reciprocal), quantity = 15000, takeLimit = Some(3))
      val taker = Order(userId = 888, id = 3, price = Some(4000), quantity = 10, timestamp = Some(0))

      val result1 = manager.addOrderToMarket(makerSide, maker1)
      result1 mustEqual OrderSubmitted(
        OrderInfo(makerSide, maker1.copy(refund = Some(Refund(OverCharged, 5500))), 0, 0, Pending), Nil)

      manager.addOrderToMarket(makerSide, maker2)
      val result2 = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker1 = maker1.copy(quantity = maker1.quantity - 5500 - 4500, takeLimit = Some(0), inAmount = 1)
      val updatedMaker2 = maker2.copy(quantity = maker2.quantity - 15000, takeLimit = Some(0), inAmount = 3)
      val updatedTaker = taker.copy(quantity = 6, inAmount = 19500)

      result2 mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 4, 19500, PartiallyExecuted, Some(0)),
        Seq(
          Transaction(3001, 0, takerSide, taker --> taker.copy(quantity = 7, inAmount = 15000),
            maker2 --> updatedMaker2),
          Transaction(3002, 0, takerSide, taker.copy(quantity = 7, inAmount = 15000) --> updatedTaker,
            maker1.copy(quantity = maker1.quantity - 5500) --> updatedMaker1)))

      manager.orderMap mustEqual Map(3 -> taker.copy(quantity = 6, inAmount = 19500))
      manager.orderPool(takerSide) mustEqual SortedSet(updatedTaker)
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
    }
  }

  "MarketManager" should {
    "market-price orders can't exist in an empty market" in {
      val manager = new MarketManager(Btc ~> Cny)

      val maker1 = Order(userId = 888L, id = 1, price = None, quantity = 100)
      val maker2 = Order(userId = 888L, id = 2, price = None, quantity = 500)

      val result1 = manager.addOrderToMarket(makerSide, maker1)
      val result2 = manager.addOrderToMarket(makerSide, maker2)

      val updatedMaker1 = maker1
      val updatedMaker2 = maker2

      result1 mustEqual OrderSubmitted(OrderInfo(makerSide, updatedMaker1.copy(refund = Some(Refund(AutoCancelled, 100))), 0, 0,
        CancelledByMarket, None), Nil)
      result2 mustEqual OrderSubmitted(OrderInfo(makerSide, updatedMaker2.copy(refund = Some(Refund(AutoCancelled, 500))), 0, 0,
        CancelledByMarket, None), Nil)

      manager.orderMap mustEqual Map()
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "NOT match new market-price taker order" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 888L, id = 1, price = None, quantity = 100)
      val taker = Order(userId = 888L, id = 2, price = None, quantity = 100)

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      result.txs mustEqual Nil

      manager.orderMap mustEqual Map()
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "match new market-price taker order against existing limit-price maker orders and fully execute both orders " +
      "if quantity equals" in {
        val manager = new MarketManager(Btc ~> Cny)

        val maker = Order(userId = 777L, id = 1, price = Some(1), quantity = 100)
        val taker = Order(userId = 888L, id = 2, price = None, quantity = 100)

        manager.addOrderToMarket(makerSide, maker)
        val result = manager.addOrderToMarket(takerSide, taker)

        val updatedMaker = maker.copy(quantity = 0, inAmount = 100)
        val updatedTaker = taker.copy(quantity = 0, inAmount = 100)

        result mustEqual OrderSubmitted(
          OrderInfo(takerSide, taker, 100, 100, FullyExecuted, Some(0)),
          Seq(
            Transaction(2001, 0, takerSide,
              taker --> updatedTaker,
              maker --> updatedMaker)))

        manager.orderMap.size mustEqual 0
        manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
        manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
      }

    "match new market-price taker order against existing limit-price maker orders and fully execute taker orders " +
      "if its quantity is smaller" in {
        val manager = new MarketManager(Btc ~> Cny)

        val maker = Order(userId = 777L, id = 1, price = Some(1), quantity = 100)
        val taker = Order(userId = 888L, id = 2, price = None, quantity = 10)

        manager.addOrderToMarket(makerSide, maker)
        val result = manager.addOrderToMarket(takerSide, taker)

        val updatedMaker = maker.copy(quantity = 90, inAmount = 10)
        val updatedTaker = taker.copy(quantity = 0, inAmount = 10)

        result mustEqual OrderSubmitted(
          OrderInfo(takerSide, taker, 10, 10, FullyExecuted, Some(0)),
          Seq(Transaction(2001, 0, takerSide,
            taker --> updatedTaker,
            maker --> updatedMaker)))

        manager.orderMap mustEqual Map(1 -> updatedMaker)
        manager.orderPool(makerSide) mustEqual SortedSet(updatedMaker)
        manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]

      }

    "match new market-price taker order against existing limit-price maker orders and fully execute maker orders " +
      "if its quantity is smaller" in {
        val manager = new MarketManager(Btc ~> Cny)
        val maker = Order(userId = 777L, id = 1, price = Some(1), quantity = 10)
        val taker = Order(userId = 888L, id = 2, price = None, quantity = 100)

        manager.addOrderToMarket(makerSide, maker)
        val result = manager.addOrderToMarket(takerSide, taker)

        val updatedMaker = maker.copy(quantity = 0, inAmount = 10)
        val updatedTaker = taker.copy(quantity = 90, inAmount = 10)

        result mustEqual OrderSubmitted(
          OrderInfo(takerSide, taker, 10, 10, PartiallyExecutedThenCancelledByMarket, Some(0)),
          Seq(Transaction(2001, 0, takerSide, taker --> updatedTaker.copy(refund = Some(Refund(AutoCancelled, 90))),
            maker --> updatedMaker)))

        manager.orderMap mustEqual Map()
        manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
        manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
      }

    "match new market-price taker order against multiple existing limit-price maker orders and fully execute " +
      "taker order if its quantity is smaller" in {
        val manager = new MarketManager(Btc ~> Cny)

        val maker1 = Order(userId = 666L, id = 1, price = Some(1), quantity = 100) // lower price
        val maker2 = Order(userId = 777L, id = 2, price = Some(0.5), quantity = 100) // higher price
        val taker = Order(userId = 888L, id = 3, price = None, quantity = 120)

        manager.addOrderToMarket(makerSide, maker1)
        manager.addOrderToMarket(makerSide, maker2)
        val result = manager.addOrderToMarket(takerSide, taker)

        val updatedMaker1 = maker1.copy(quantity = 30, inAmount = 70)
        val updatedMaker2 = maker2.copy(quantity = 0, inAmount = 50)
        val updatedTaker = taker.copy(id = 3, quantity = 0, inAmount = 170)

        result mustEqual OrderSubmitted(
          OrderInfo(takerSide, taker, 120, 170, FullyExecuted, Some(0)),
          Seq(
            Transaction(3001, 0, takerSide,
              taker --> taker.copy(quantity = 70, inAmount = 100), maker2 --> updatedMaker2),
            Transaction(3002, 0, takerSide,
              taker.copy(quantity = 70, inAmount = 100) --> updatedTaker, maker1 --> updatedMaker1)))

        manager.orderMap mustEqual Map(1 -> updatedMaker1) //  100 x 0.5 + 100 x 1 - 120 = 30
        manager.orderPool(makerSide) mustEqual SortedSet(updatedMaker1)
        manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
      }

    "match new market-price taker order against multiple existing limit-price maker orders and fully execute " +
      "all maker orders if their combined quantity is smaller" in {
        val manager = new MarketManager(Btc ~> Cny)
        val maker1 = Order(userId = 666L, id = 1, price = Some(1), quantity = 20) // lower price
        val maker2 = Order(userId = 777L, id = 2, price = Some(0.5), quantity = 100) // higher price
        val taker = Order(userId = 888L, id = 3, price = None, quantity = 120)

        manager.addOrderToMarket(makerSide, maker1)
        manager.addOrderToMarket(makerSide, maker2)
        val result = manager.addOrderToMarket(takerSide, taker)

        val updatedMaker1 = maker1.copy(quantity = 0, inAmount = 20)
        val updatedMaker2 = maker2.copy(quantity = 0, inAmount = 50)
        val updatedTaker = taker.copy(quantity = 50, inAmount = 120)

        result mustEqual OrderSubmitted(
          OrderInfo(takerSide, taker, 70, 120, PartiallyExecutedThenCancelledByMarket, Some(0)),
          Seq(
            Transaction(3001, 0, takerSide,
              taker --> taker.copy(quantity = 70, inAmount = 100), maker2 --> updatedMaker2),
            Transaction(3002, 0, takerSide,
              taker.copy(quantity = 70, inAmount = 100) --> updatedTaker.copy(refund = Some(Refund(AutoCancelled, 50))),
              maker1 --> updatedMaker1)))

        manager.orderMap mustEqual Map()
        manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
        manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
      }
  }

  "MarketManager" should {
    "match new limit-price taker order against the highest limit-price maker order" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 666, id = 1, price = Some(1), quantity = 20) // lower price
      val maker2 = Order(userId = 777, id = 2, price = Some(0.5), quantity = 100) // higher price
      val taker = Order(userId = 888L, id = 3, price = Some(1), quantity = 10)

      manager.addOrderToMarket(makerSide, maker1)
      manager.addOrderToMarket(makerSide, maker2)

      val result = manager.addOrderToMarket(takerSide, taker)
      val updatedMaker2 = maker2.copy(quantity = 80, inAmount = 10)
      val updatedTaker = taker.copy(quantity = 0, inAmount = 20)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 10, 20, FullyExecuted, Some(0)),
        Seq(Transaction(3001, 0, takerSide, taker --> updatedTaker, maker2 --> updatedMaker2)))

      manager.orderMap mustEqual Map(1 -> maker1, 2 -> updatedMaker2)
      manager.orderPool(makerSide) mustEqual SortedSet(maker1, updatedMaker2)
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "match new limit-price taker order fully against multiple limit-price maker orders" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 666, id = 1, price = Some(1), quantity = 20, timestamp = Some(11111)) // lower price
      val maker2 = Order(userId = 777, id = 2, price = Some(0.5), quantity = 100, timestamp = Some(22222)) // higher price
      val taker = Order(userId = 888L, id = 3, price = Some(1), quantity = 60, timestamp = Some(33333))

      manager.addOrderToMarket(makerSide, maker1)
      manager.addOrderToMarket(makerSide, maker2)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker1 = maker1.copy(quantity = 10, inAmount = 10)
      val updatedMaker2 = maker2.copy(quantity = 0, inAmount = 50)
      val updatedTaker = taker.copy(quantity = 0, inAmount = 110)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 60, 110, FullyExecuted, Some(33333)),
        Seq(
          Transaction(3001, 33333, takerSide,
            taker --> taker.copy(quantity = 10, inAmount = 100), maker2 --> updatedMaker2),
          Transaction(3002, 33333, takerSide,
            taker.copy(quantity = 10, inAmount = 100) --> updatedTaker, maker1 --> updatedMaker1)))

      manager.orderMap mustEqual Map(1 -> maker1.copy(quantity = 10, inAmount = 10))
      manager.orderPool(makerSide) mustEqual SortedSet(maker1.copy(quantity = 10))
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "match new limit-price taker order partially against multiple limit-price maker orders" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 666, id = 1, price = Some(0.5), quantity = 20) // lower price
      val maker2 = Order(userId = 777, id = 2, price = Some(0.4), quantity = 100) // higher price
      val taker = Order(userId = 888L, id = 3, price = Some(2), quantity = 90)

      manager.addOrderToMarket(makerSide, maker1)
      manager.addOrderToMarket(makerSide, maker2)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker1 = maker1.copy(quantity = 0, inAmount = 10)
      val updatedMaker2 = maker2.copy(quantity = 0, inAmount = 40)
      val updatedTaker = taker.copy(quantity = 40, inAmount = 120)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 50, 120, PartiallyExecuted, Some(0)),
        Seq(
          Transaction(3001, 0, takerSide,
            taker --> taker.copy(quantity = 50, inAmount = 100), maker2 --> updatedMaker2),
          Transaction(3002, 0, takerSide,
            taker.copy(quantity = 50, inAmount = 100) --> updatedTaker, maker1 --> updatedMaker1)))

      manager.orderMap mustEqual Map(3 -> updatedTaker) // 90 - 100x0.4 - 20x0.5
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet(updatedTaker)
    }

    "match new limit-price taker order fully against existing market-price maker order 1" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker1 = Order(userId = 666, id = 1, price = None, quantity = 20) // high priority
      val maker2 = Order(userId = 777, id = 2, price = None, quantity = 100) // low priority
      val taker = Order(userId = 888L, id = 3, price = Some(2), quantity = 5)

      manager.addOrderToMarket(makerSide, maker1)
      manager.addOrderToMarket(makerSide, maker2)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker1 = maker1.copy(quantity = 10)
      val updatedTaker = taker.copy(quantity = 0)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 0, 0, Pending, None),
        Seq())

      manager.orderMap mustEqual Map(3 -> taker)
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet(taker)
    }
  }

  "MarketManager" should {
    "be able to handle dust" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 1, id = 1, price = Some(500.1), quantity = 1)
      val taker = Order(userId = 5, id = 2, price = None, quantity = 900)

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker = maker.copy(quantity = 0, inAmount = 500)
      val updatedTaker = taker.copy(quantity = 400, inAmount = 1)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 500, 1, PartiallyExecutedThenCancelledByMarket, Some(0)),
        Seq(Transaction(2001, 0, takerSide, taker --> updatedTaker.copy(refund = Some(Refund(AutoCancelled, 400))),
          maker --> updatedMaker)))
    }

    "be able to handle dust" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 1, id = 1, price = Some(500.9), quantity = 1)
      val taker = Order(userId = 5, id = 2, price = None, quantity = 900)

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker = maker.copy(quantity = 0, inAmount = 501)
      val updatedTaker = taker.copy(quantity = 399, inAmount = 1)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 501, 1, PartiallyExecutedThenCancelledByMarket, Some(0)),
        Seq(Transaction(2001, 0, takerSide, taker --> updatedTaker.copy(refund = Some(Refund(AutoCancelled, 399))),
          maker --> updatedMaker)))
    }

    "be able to handle dust when price is really small" in {
      val manager = new MarketManager(Btc ~> Cny)
      val maker = Order(userId = 1, id = 1, price = Some(0.15), quantity = 1000)
      val taker = Order(userId = 5, id = 2, price = None, quantity = 180)

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      val updatedMaker = maker.copy(quantity = 0, inAmount = 150)
      val updatedTaker = taker.copy(quantity = 30, inAmount = 1000)

      result mustEqual OrderSubmitted(
        OrderInfo(takerSide, taker, 150, 1000, PartiallyExecutedThenCancelledByMarket, Some(0)),
        Seq(Transaction(2001, 0, takerSide, taker --> updatedTaker.copy(refund = Some(Refund(AutoCancelled, 30))),
          maker --> updatedMaker)))
    }
  }

  "MarketManager" should {
    "drop order which has onlyTaker flag" in {
      val manager = new MarketManager(Btc ~> Cny)
      val taker = Order(userId = 888L, id = 1, price = Some(3000), quantity = 100, onlyTaker = Some(true))

      val result = manager.addOrderToMarket(takerSide, taker)

      result.txs mustEqual Nil
      manager.orderMap mustEqual Map()
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }

    "drop order which has onlyTaker flag after match" in {
      val side = (Btc ~> Cny)
      val manager = new MarketManager(side)
      val maker = Order(userId = 888L, id = 1, price = Some(5000 reciprocal), quantity = 100 * 5000)
      val taker = Order(userId = 888L, id = 2, price = Some(3000), quantity = 1000, onlyTaker = Some(true))

      manager.addOrderToMarket(makerSide, maker)
      val result = manager.addOrderToMarket(takerSide, taker)

      result mustEqual OrderSubmitted(
        OrderInfo(side, taker, 100, 500000, PartiallyExecutedThenCancelledByMarket, Some(0)),
        Seq(Transaction(2001, 0, side,
          taker --> taker.copy(quantity = 900, inAmount = 5000 * 100, refund = Some(Refund(AutoCancelled, 900))),
          maker --> maker.copy(quantity = 0, inAmount = 100)))
      )

      manager.orderMap mustEqual Map()
      manager.orderPool(makerSide) mustEqual SortedSet.empty[Order]
      manager.orderPool(takerSide) mustEqual SortedSet.empty[Order]
    }
  }
}
