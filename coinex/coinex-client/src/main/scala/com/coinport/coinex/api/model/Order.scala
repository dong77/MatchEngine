/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.model

import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import scala.Some

object Operations extends Enumeration {
  type Operation = Value
  val Buy, Sell = Value

  implicit def reverse(operation: Operation) = operation match {
    case Buy => Sell
    case Sell => Buy
  }
}

import com.coinport.coinex.api.model.Operations._

// Order from the view of users
case class UserOrder(
    uid: String,
    operation: Operation,
    subject: String,
    currency: String,
    price: Option[Double],
    amount: Option[Double],
    total: Option[Double],
    status: Int = 0,
    id: String = "0",
    submitTime: Long = 0L,
    finishedQuantity: Double = 0.0,
    finishedAmount: Double = 0.0) {
  //  buy: money   out, subject in
  // sell: subject out, money   in
  private val currency1: Currency = subject
  private val currency2: Currency = currency
  val marketSide = operation match {
    case Buy => currency2 ~> currency1
    case Sell => currency1 ~> currency2
  }

  def toDoSubmitOrder(): DoSubmitOrder = {
    operation match {
      case Buy =>
        // convert price
        val newPrice = price map {
          value => value.internalValue(marketSide.reverse) reciprocal
        }
        // regard total as quantity
        val quantity: Long = total match {
          case Some(t) => t.internalValue(currency2)
          case None => (BigDecimal(amount.get) * BigDecimal(price.get)).doubleValue().internalValue(currency2)
        }
        val limit = amount.map(_.internalValue(currency1))
        DoSubmitOrder(marketSide, Order(uid.toLong, id.toLong, quantity, newPrice, limit))
      case Sell =>
        // TODO: handle None total or price
        val newPrice: Option[RDouble] = price.map(p => p.internalValue(marketSide))
        val quantity: Long = amount match {
          case Some(a) => a.internalValue(currency1)
          case None =>
            if (total.isDefined && price.isDefined) {
              val totalValue = BigDecimal(total.get) / BigDecimal(price.get)
              totalValue.doubleValue().internalValue(currency1)
            } else 0
        }
        val limit = total map {
          total => total.internalValue(currency2)
        }
        DoSubmitOrder(marketSide, Order(uid.toLong, id.toLong, quantity, newPrice, limit))
    }
  }
}

object UserOrder {
  def fromOrderInfo(orderInfo: OrderInfo): UserOrder = {
    // all are sell-orders
    val marketSide = orderInfo.side
    val order = orderInfo.order

    val unit1 = marketSide._1
    val unit2 = marketSide._2
    if (marketSide.ordered) {
      // sell
      val price: Option[Double] = order.price.map {
        p: RDouble => p.value.externalValue(marketSide)
      }
      val amount: Option[Double] = Some(order.quantity.externalValue(unit1))
      val total: Option[Double] = order.takeLimit.map(t => t.externalValue(unit2))

      // finished quantity = out
      val finishedQuantity = orderInfo.outAmount.externalValue(unit1)
      // finished amount = in
      val finishedAmount = orderInfo.inAmount.externalValue(unit2)

      val status = orderInfo.status
      val id = order.id
      val timestamp = order.timestamp.getOrElse(0L)

      UserOrder(order.userId.toString, Sell, unit1, unit2, price, amount, total, status.value, id.toString, timestamp)
        .copy(finishedQuantity = finishedQuantity, finishedAmount = finishedAmount)
    } else {
      // buy
      val price: Option[Double] = order.price.map {
        p: RDouble => p.reciprocal.value.externalValue(marketSide.reverse)
      }

      val amount = order.takeLimit.map(t => t.externalValue(unit2))
      val total = Some(order.quantity.externalValue(unit1))

      // finished quantity = in
      val finishedQuantity = orderInfo.inAmount.externalValue(unit2)

      // finished amount = out
      val finishedAmount = orderInfo.outAmount.externalValue(unit1)

      val status = orderInfo.status
      val id = order.id
      val timestamp = order.timestamp.getOrElse(0L)

      UserOrder(order.userId.toString, Buy, unit2, unit1, price, amount, total, status.value, id.toString, timestamp)
        .copy(finishedQuantity = finishedQuantity, finishedAmount = finishedAmount)
    }
  }
}

case class ApiOrder(uid: String,
  operation: String,
  subject: String,
  currency: String,
  price: Option[PriceObject],
  amount: Option[CurrencyObject],
  total: Option[CurrencyObject],
  status: Int = 0,
  id: String,
  submitTime: Long,
  finishedQuantity: CurrencyObject,
  finishedAmount: CurrencyObject)

object ApiOrder {
  def fromOrderInfo(orderInfo: OrderInfo): ApiOrder = {
    // all are sell-orders
    val marketSide = orderInfo.side
    val order = orderInfo.order

    val unit1 = marketSide._1
    val unit2 = marketSide._2
    if (marketSide.ordered) {
      // sell
      val price: Option[PriceObject] = order.price.map {
        p: RDouble => PriceObject(marketSide, p.value)
      }
      val amount: Option[CurrencyObject] = Some(CurrencyObject(unit1, order.quantity))
      val total: Option[CurrencyObject] = order.takeLimit.map(t => CurrencyObject(unit2, t))

      // finished quantity = out
      val finishedQuantity = CurrencyObject(unit1, orderInfo.outAmount)
      // finished amount = in
      val finishedAmount = CurrencyObject(unit2, orderInfo.inAmount)

      val status = orderInfo.status
      val id = order.id
      val timestamp = order.timestamp.getOrElse(0L)

      ApiOrder(order.userId.toString, "Sell", unit1, unit2, price, amount, total, status.value, id.toString, timestamp, finishedQuantity, finishedAmount)
    } else {
      // buy
      val price: Option[PriceObject] = order.price.map {
        p: RDouble => PriceObject(marketSide.reverse, p.reciprocal.value)
      }

      val amount = order.takeLimit.map(t => CurrencyObject(unit2, t))
      val total = Some(CurrencyObject(unit1, order.quantity))

      // finished quantity = in
      val finishedQuantity = CurrencyObject(unit2, orderInfo.inAmount)

      // finished amount = out
      val finishedAmount = CurrencyObject(unit1, orderInfo.outAmount)

      val status = orderInfo.status
      val id = order.id
      val timestamp = order.timestamp.getOrElse(0L)

      ApiOrder(order.userId.toString, "Buy", unit2, unit1, price, amount, total, status.value, id.toString, timestamp, finishedQuantity, finishedAmount)
    }
  }
}
