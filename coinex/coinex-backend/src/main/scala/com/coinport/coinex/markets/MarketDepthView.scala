/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.markets

import akka.event.LoggingReceive
import akka.persistence.Persistent
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import com.coinport.coinex.common._
import Implicits._
import scala.collection.SortedSet
import scala.collection.mutable.{ ListBuffer, Map }

class MarketDepthView(market: MarketSide) extends ExtendedView {
  override val processorId = MARKET_PROCESSOR << market
  override val viewId = MARKET_DEPTH_VIEW << market

  case class Cached(depth: Int, asks: Seq[MarketDepthItem], bids: Seq[MarketDepthItem])
  case class CachedByPrice(askPrice: Double, bidPrice: Double, asks: Seq[MarketDepthItem], bids: Seq[MarketDepthItem])

  val manager = new MarketManager(market)
  private var cacheMap = Map.empty[MarketSide, Cached]
  private var cacheByPriceMap = Map.empty[MarketSide, CachedByPrice]

  def receive = LoggingReceive {
    case Persistent(DoCancelOrder(_, orderId, userId), _) =>
      manager.removeOrder(orderId, userId)
      cacheMap = Map.empty[MarketSide, Cached]
      cacheByPriceMap = Map.empty[MarketSide, CachedByPrice]

    case Persistent(OrderFundFrozen(side, order: Order), _) =>
      if (manager.isOrderPriceInGoodRange(side, order.price)) {
        manager.addOrderToMarket(side, order)
        cacheMap = Map.empty[MarketSide, Cached]
        cacheByPriceMap = Map.empty[MarketSide, CachedByPrice]
      }

    case QueryMarketDepth(side, depth) =>
      cacheMap.get(side) match {
        case Some(cached) if cached.depth >= depth =>
        case _ => cacheMap += side -> getDepthData(side, depth)
      }
      sender ! QueryMarketDepthResult(MarketDepth(side, cacheMap(side).asks.take(depth),
        cacheMap(side).bids.take(depth)))

    case QueryMarketDepthByPrice(side, askPrice, bidPrice) =>
      cacheByPriceMap.get(side) match {
        case Some(cached) if (cached.askPrice >= askPrice && cached.bidPrice <= bidPrice) =>
        case _ => cacheByPriceMap += side -> getDepthData(side, askPrice, bidPrice)
      }
      sender ! QueryMarketDepthResult(MarketDepth(side,
        cutDepthItemsByPrice(cacheByPriceMap(side).asks, askPrice, true),
        cutDepthItemsByPrice(cacheByPriceMap(side).bids, bidPrice, false)))

    case DoSimulateOrderSubmission(DoSubmitOrder(side, order)) =>
      val state = manager.getState()
      val orderSubmitted = manager.addOrderToMarket(side, order)
      manager.loadState(state)
      sender ! OrderSubmissionSimulated(orderSubmitted)
  }

  private def cutDepthItemsByPrice(
    items: Seq[MarketDepthItem], price: Double, isAsk: Boolean): Seq[MarketDepthItem] = {
    val takenNum = if (isAsk) items.indexWhere(_.price > price) else items.indexWhere(_.price < price)
    if (takenNum < 0) items else items.slice(0, takenNum)
  }

  private def orderToDepthItem(order: Order, isAsk: Boolean) = {
    if (isAsk) MarketDepthItem(order.price.get.value, order.maxOutAmount(order.price.get), Some(List(order.id)))
    /*bid*/
    else MarketDepthItem(order.price.get.reciprocal.value, order.maxInAmount(order.price.get), Some(List(order.id)))
  }

  private def takeDepthItemsByDepth(orders: SortedSet[Order], depth: Int, isAsk: Boolean) = {
    val buffer = new ListBuffer[MarketDepthItem]
    var index = 0
    while (buffer.size < depth && index < orders.size) {
      val order = orders.view(index, index + 1).head
      val item = orderToDepthItem(order, isAsk)
      if (buffer.isEmpty || buffer.last.price != item.price) buffer += item
      else {
        val last = buffer.last
        val orderIds = last.orderIds.getOrElse(List.empty[Long]) ++ item.orderIds.getOrElse(List.empty[Long])
        buffer.trimEnd(1)
        buffer += last.copy(quantity = last.quantity + item.quantity,
          orderIds = if (orderIds.size > 0) Some(orderIds) else None)
      }
      index += 1
    }
    buffer.toSeq
  }

  private def takeDepthItemsByPrice(orders: SortedSet[Order], price: Double, isAsk: Boolean) = {
    def priceInRange(p: Double): Boolean = if (isAsk) p <= price else p >= price

    val buffer = new ListBuffer[MarketDepthItem]
    var index = 0
    var continue = true
    while (index < orders.size && continue) {
      val order = orders.view(index, index + 1).head
      val item = orderToDepthItem(order, isAsk)
      if (priceInRange(item.price)) {
        if (buffer.isEmpty || buffer.last.price != item.price) buffer += item
        else {
          val last = buffer.last
          val orderIds = last.orderIds.getOrElse(List.empty[Long]) ++ item.orderIds.getOrElse(List.empty[Long])
          buffer.trimEnd(1)
          buffer += last.copy(quantity = last.quantity + item.quantity,
            orderIds = if (orderIds.size > 0) Some(orderIds) else None)
        }
        index += 1
      } else {
        continue = false
      }
    }
    buffer.toSeq
  }

  private def getDepthData(side: MarketSide, depth: Int) = {
    val asks = takeDepthItemsByDepth(manager.orderPool(side), depth, true)
    val bids = takeDepthItemsByDepth(manager.orderPool(side.reverse), depth, false)

    Cached(depth, asks, bids)
  }

  private def getDepthData(side: MarketSide, askPrice: Double, bidPrice: Double): CachedByPrice = {
    val asks = takeDepthItemsByPrice(manager.orderPool(side), askPrice, true)
    val bids = takeDepthItemsByPrice(manager.orderPool(side.reverse), bidPrice, false)

    CachedByPrice(askPrice, bidPrice, asks, bids)
  }
}
