/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 * All classes here are case-classes or case-objects. This is required since we are
 * maintaining an in-memory state that's immutable, so that while snapshot is taken,
 * the in-memory state can still be updated.
 */

package com.coinport.coinex.data

import com.coinport.coinex.common._
import com.coinport.coinex.common.Constants

class RichDouble(d: Double) {
  def reciprocal = RDouble(d, true)
  def !!! = {
    if (d < 1.0) scaled(26)
    else if (d > 1.0) scaled(8)
    else d
  }

  def scaled(s: Int) = BigDecimal(d).setScale(s, BigDecimal.RoundingMode.HALF_UP).toDouble
}

class RichRDouble(raw: RDouble) {
  def value = valueOf(raw)
  def reciprocal = raw.copy(rm = !raw.rm)

  def >(another: RDouble) = value > valueOf(another)
  def ==(another: RDouble) = value == valueOf(another)
  def <(another: RDouble) = value < valueOf(another)

  def *(another: RDouble): Double =
    if (raw.rm) {
      if (another.rm) 1 / (raw.input * another.input) else another.input / raw.input
    } else {
      if (another.rm) raw.input / another.input else another.input * raw.input
    }

  private def valueOf(rd: RDouble): Double = if (rd.rm) 1 / rd.input else rd.input
}

class RichCurrency(raw: Currency) {
  def ~>(another: Currency) = MarketSide(raw, another)
  def <~(another: Currency) = MarketSide(another, raw)
}

class RichMarketSide(raw: MarketSide) {
  def reverse = MarketSide(raw.inCurrency, raw.outCurrency)
  def S = "%s-%s".format(raw.outCurrency, raw.inCurrency).toUpperCase
  def s = "%s-%s".format(raw.outCurrency, raw.inCurrency).toLowerCase
  def market = Market(raw.outCurrency, raw.inCurrency)
  def ordered = raw.inCurrency.getValue < raw.outCurrency.getValue
  def normalized = if (ordered) raw else reverse
}

class RichOrder(raw: Order) {
  implicit def rdouble2Rich(raw: RDouble) = new RichRDouble(raw)

  def vprice = raw.price.getOrElse(RDouble(0.0, false))

  def maxOutAmount(price: RDouble): Long = {
    val quantity = raw.refund match {
      case Some(refund) => raw.quantity - refund.amount
      case None => raw.quantity
    }
    raw.takeLimit match {
      // taker sell 1 BTC in price 2000 with takeLimit 2000, maker buy 10 BTC in price 10000
      // we need sell taker's 1 BTC in price 10000 even the limit quantity is 2000 / 10000 = 0.2
      // so need using "ceil"
      case Some(limit) if limit <= 0 => 0
      case Some(limit) if price.reciprocal.value * limit < quantity =>
        Math.ceil(price.reciprocal.value * limit).toLong
      case _ => quantity
    }
  }

  def maxInAmount(price: RDouble): Long = {
    val quantity = raw.refund match {
      case Some(refund) => raw.quantity - refund.amount
      case None => raw.quantity
    }
    raw.takeLimit match {
      case Some(limit) if limit <= 0 => 0
      case Some(limit) if limit > 0 && limit < price.value * quantity => limit
      case _ =>
        // this check ensure that the amount couldn't buyed definately be a dust in future check
        val rounded = Math.round(quantity * price.value)
        if ((price.reciprocal.value * rounded).toLong > quantity) rounded.toLong - 1 else rounded.toLong
    }
  }

  def hitTakeLimit = raw.takeLimit != None && raw.takeLimit.get <= 0

  def soldOut = if (raw.price.isDefined) raw.quantity * vprice.value < 1 else raw.quantity == 0

  def isDust = raw.price.isDefined && raw.quantity != 0 && raw.quantity * vprice.value < 1

  def canBecomeMaker = !soldOut && !hitTakeLimit && raw.price.isDefined && !raw.onlyTaker.getOrElse(false)

  def -->(another: Order) = OrderUpdate(raw, another)
}

class RichOrderInfo(raw: OrderInfo) {
  def remainingQuantity = raw.order.quantity - raw.outAmount
}

class RichOrderUpdate(raw: OrderUpdate) {
  def userId = raw.previous.userId
  def id = raw.previous.id
  def price = raw.previous.price
  def outAmount = raw.previous.quantity - raw.current.quantity
}

class RichTransaction(raw: Transaction) {
}

class RichOrderSubmitted(raw: OrderSubmitted) {
  def hasTransaction = raw.txs != null && raw.txs.nonEmpty
}

class RichCashAccount(raw: CashAccount) {
  def total: Long = raw.available + raw.locked + raw.pendingWithdrawal

  def +(another: CashAccount): CashAccount = {
    if (raw.currency != another.currency)
      throw new IllegalArgumentException("Cannot add different currency accounts")
    CashAccount(raw.currency,
      raw.available + another.available,
      raw.locked + another.locked,
      raw.pendingWithdrawal + another.pendingWithdrawal)
  }

  def -(another: CashAccount): CashAccount = {
    if (raw.currency != another.currency)
      throw new IllegalArgumentException("Cannot minus different currency accounts")
    CashAccount(raw.currency,
      raw.available - another.available,
      raw.locked - another.locked,
      raw.pendingWithdrawal - another.pendingWithdrawal)
  }

  def isValid = (raw.available >= 0 && raw.locked >= 0 && raw.pendingWithdrawal >= 0)
}

class RichConstRole(v: ConstantRole.Value) {
  def << = v.toString.toLowerCase
}

class RichMarketRole(v: MarketRole.Value) {
  implicit def marketSide2Rich(raw: MarketSide) = new RichMarketSide(raw)

  def <<(side: MarketSide) = v.toString.toLowerCase + "_" + side.s
}

class RichBitwayRole(v: BitwayRole.Value) {
  def <<(currency: Currency) = v.toString.toLowerCase + "_" + currency.toString.toLowerCase
}

class RichPersistentId(v: PersistentId.Value) {
  implicit def marketSide2Rich(raw: MarketSide) = new RichMarketSide(raw)

  def << : String = v.toString.toLowerCase
  def <<(side: MarketSide): String = << + "_" + side.s
  def <<(currency: Currency): String = << + "_" + currency.toString.toLowerCase
}

class RichMarketSideList(markets: Seq[MarketSide]) {
  def toCryptoCurrencySet = markets.flatMap(i => List(
    i.inCurrency, i.outCurrency)).toSet.filter(_.value >= Constants.MIN_CRYPTO_CURRENCY_INDEX).toSeq
}

object Implicits {
  implicit def double2RDouble(d: Double) = RDouble(d, false)
  implicit def double2Rich(d: Double) = new RichDouble(d)

  implicit def rdouble2Rich(raw: RDouble) = new RichRDouble(raw)
  implicit def currency2Rich(raw: Currency) = new RichCurrency(raw)
  implicit def marketSide2Rich(raw: MarketSide) = new RichMarketSide(raw)
  implicit def order2Rich(raw: Order) = new RichOrder(raw)
  implicit def orderInfo2Rich(raw: OrderInfo) = new RichOrderInfo(raw)
  implicit def orderUpdate2Rich(raw: OrderUpdate) = new RichOrderUpdate(raw)
  implicit def transaction2Rich(raw: Transaction) = new RichTransaction(raw)
  implicit def orderSubmitted2Rich(raw: OrderSubmitted) = new RichOrderSubmitted(raw)
  implicit def cashAccont2Rich(raw: CashAccount) = new RichCashAccount(raw)

  implicit def constantRole2Rich(raw: ConstantRole.Value) = new RichConstRole(raw)
  implicit def marketRole2Rich(raw: MarketRole.Value) = new RichMarketRole(raw)
  implicit def bitwayRole2Rich(raw: BitwayRole.Value) = new RichBitwayRole(raw)
  implicit def persistentId2Rich(raw: PersistentId.Value) = new RichPersistentId(raw)

  implicit def string2RichMarketSide(raw: String): MarketSide = {
    if (raw == null || raw.isEmpty || raw.length < 6) {
      MarketSide(Currency.Unknown, Currency.Unknown)
    } else if (raw.contains("-")) {
      val left = Currency.valueOf(raw.split("-")(0).toLowerCase.capitalize).getOrElse(Currency.Unknown)
      val right = Currency.valueOf(raw.split("-")(1).toLowerCase.capitalize).getOrElse(Currency.Unknown)
      MarketSide(left, right)
    } else {
      val left = Currency.valueOf(raw.substring(0, 3).toLowerCase.capitalize).getOrElse(Currency.Unknown)
      val right = Currency.valueOf(raw.substring(3, 6).toLowerCase.capitalize).getOrElse(Currency.Unknown)
      MarketSide(left, right)
    }
  }
  implicit def string2RichMarket(raw: String): Market = string2RichMarketSide(raw).market
  implicit def markets2CryptoCurrencySet(markets: Seq[MarketSide]) = new RichMarketSideList(markets)
}
