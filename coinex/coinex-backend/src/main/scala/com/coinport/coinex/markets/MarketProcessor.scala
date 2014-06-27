/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.markets

import akka.actor.{ ActorRef, ActorPath }
import akka.actor.Actor.Receive
import akka.persistence._
import akka.event.LoggingReceive
import com.coinport.coinex.common.ExtendedProcessor
import com.coinport.coinex.data._
import Implicits._
import ErrorCode._
import com.coinport.coinex.common.support.ChannelSupport
import com.coinport.coinex.common.PersistentId._
// import com.coinport.coinex.debug.Debugger

class MarketProcessor(
  marketSide: MarketSide,
  accountProcessorPath: ActorPath)
    extends ExtendedProcessor with EventsourcedProcessor with ChannelSupport {

  override def processorId = MARKET_PROCESSOR << marketSide

  val channelToAccountProcessor = createChannelTo(ACCOUNT_PROCESSOR <<) // DO NOT CHANGE

  val manager = new MarketManager(marketSide)

  def receiveRecover = PartialFunction.empty[Any, Unit]

  def receiveCommand = LoggingReceive {
    case m @ DoCancelOrder(_, orderId, userId) =>
      manager.getOrderMarketSide(orderId, userId) match {
        case Some(side) => persist(m.copy(side = side))(updateState)
        case None => sender ! CancelOrderFailed(OrderNotExist)
      }

    case p @ ConfirmablePersistent(m @ OrderFundFrozen(side, order: Order), seq, _) =>
      confirm(p)

      // Update timestamp again
      val updated = m.copy(order = m.order.copy(timestamp = Some(System.currentTimeMillis)))
      persist(updated)(updateState)
  }

  def updateState: Receive = {
    case DoCancelOrder(_, orderId, userId) =>
      val (side, order) = manager.removeOrder(orderId, userId)
      val cancelled = OrderCancelled(side, order)
      channelToAccountProcessor forward Deliver(Persistent(cancelled), accountProcessorPath)

    case OrderFundFrozen(side, order: Order) =>
      if (!manager.isOrderPriceInGoodRange(side, order.price)) {
        sender ! SubmitOrderFailed(side, order, PriceOutOfRange)
        val unfrozen = OrderCancelled(side, order)
        channelToAccountProcessor forward Deliver(Persistent(unfrozen), accountProcessorPath)
      } else {
        val orderSubmitted = manager.addOrderToMarket(side, order)
        channelToAccountProcessor forward Deliver(Persistent(orderSubmitted), accountProcessorPath)
      }
  }
}
