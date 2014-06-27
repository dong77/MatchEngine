/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.model

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._

object CurrencyConversion {
  // exponent (10-based) of the factor between internal value and external value
  // Btc -> 3 means: 1 BTC(external value) equals 1 * 10E3 MBTC(internal value)
  val exponents = Map[Currency, Double](
    Btc -> 8,
    Ltc -> 8,
    Pts -> 8,
    Doge -> 8,
    Cny -> 5,
    Usd -> 5
  )

  val multipliers: Map[Currency, Double] = exponents map {
    case (k, v) =>
      k -> math.pow(10, v)
  }

  val currencyDecimals = Map[Currency, Int](
    Cny -> 4,
    Btc -> 4,
    Ltc -> 4,
    Doge -> 4
  )

  val priceDecimals = Map[MarketSide, Int](
    Btc ~> Cny -> 4,
    Ltc ~> Btc -> 4,
    Doge ~> Btc -> 8,
    Btc ~> Btc -> 1
  )

  def getExponent(currency: Currency) = exponents.get(currency).getOrElse(1.0).toInt

  def getMultiplier(currency: Currency) = multipliers.get(currency).getOrElse(1.0)
}

class CurrencyWrapper(val value: Double) {
  def externalValue(currency: Currency): Double = {
    (BigDecimal(value) / CurrencyConversion.multipliers(currency)).toDouble
  }

  def internalValue(currency: Currency): Long = {
    (BigDecimal(value) * CurrencyConversion.multipliers(currency)).toLong
  }

  def E(currency: Currency) = externalValue(currency)

  def I(currency: Currency) = internalValue(currency)
}

class PriceWrapper(val value: Double) {
  def externalValue(marketSide: MarketSide): Double = {
    val exp = CurrencyConversion.exponents(marketSide._1) - CurrencyConversion.exponents(marketSide._2)

    (value * math.pow(10, exp)).!!!
  }

  def internalValue(marketSide: MarketSide): Double = {
    val exp = CurrencyConversion.exponents(marketSide._2) - CurrencyConversion.exponents(marketSide._1)

    (value * math.pow(10, exp)).!!!
  }

  def E(marketSide: MarketSide) = externalValue(marketSide)

  def I(marketSide: MarketSide) = internalValue(marketSide)
}

case class CurrencyObject(currency: String, value_int: Long, value: Double, display: String, display_short: String)

object CurrencyObject {
  def apply(currency: Currency, value_int: Long): CurrencyObject = {
    val externalValue = value_int.externalValue(currency)
    CurrencyObject(currency, value_int, externalValue, format(externalValue, currency), formatShort(externalValue))
  }

  def format(value: Double, currency: Currency): String = {
    val decimal = CurrencyConversion.currencyDecimals(currency)
    val patten = "%." + decimal + "f"
    patten.format(value)
  }

  def formatShort(value: Double): String = "%.2f".format(value)
}

case class PriceObject(item: String, currency: String, value_int: Double, value: Double, display: String, display_short: String)

object PriceObject {
  def apply(side: MarketSide, value_int: Double): PriceObject = {
    val externalValue: Double = value_int.externalValue(side)
    PriceObject(side._1, side._2, value_int, externalValue, format(externalValue, side), formatShort(externalValue))
  }

  def format(value: Double, side: MarketSide): String = {
    val decimal = CurrencyConversion.priceDecimals(side)
    val patten = "%." + decimal + "f"
    patten.format(value)
  }

  def formatShort(value: Double): String = "%.2f".format(value)
}
