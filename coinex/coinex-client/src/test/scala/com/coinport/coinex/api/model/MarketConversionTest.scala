/**
 * Copyright (C) 2014 Coinport Inc.
 */
package com.coinport.coinex.api.model

import org.specs2.mutable._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data._

class MarketConversionTest extends Specification {
  "market conversions" should {
    "market depth conversion" in {
      val bids = List(
        MarketDepthItem(4500.0.internalValue(Btc ~> Cny), 1.5.internalValue(Btc)),
        MarketDepthItem(3000.0.internalValue(Btc ~> Cny), 2.0.internalValue(Btc)),
        MarketDepthItem(2000.0.internalValue(Btc ~> Cny), 3.0.internalValue(Btc))
      )
      val asks = List(
        MarketDepthItem(5500.0.internalValue(Btc ~> Cny), 4.5.internalValue(Btc)),
        MarketDepthItem(6000.0.internalValue(Btc ~> Cny), 5.0.internalValue(Btc)),
        MarketDepthItem(7000.0.internalValue(Btc ~> Cny), 6.0.internalValue(Btc))
      )

      val backendObj = MarketDepth(Btc ~> Cny, asks = asks, bids = bids)
      val marketDepth = fromMarketDepth(backendObj)

      marketDepth.asks mustEqual List(
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 5.5), CurrencyObject(Btc, 450000000)),
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 6.0), CurrencyObject(Btc, 500000000)),
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 7.0), CurrencyObject(Btc, 600000000)))

      marketDepth.bids mustEqual List(
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 4.5), CurrencyObject(Btc, 150000000)),
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 3.0), CurrencyObject(Btc, 200000000)),
        ApiMarketDepthItem(PriceObject(Btc ~> Cny, 2.0), CurrencyObject(Btc, 300000000)))
    }

    "market conversion" in {
      Market(Btc, Ltc) mustEqual Market(Ltc, Btc)

      Market(Btc, Usd).toString mustEqual "BTC-USD"
      Market(Usd, Btc).toString mustEqual "BTC-USD"

      var market: Market = "LTC-BTC"
      market mustEqual Market(Ltc, Btc)
      market = "XXC-XXX"
      market mustEqual Market(Unknown, Unknown)

      market = "LTCBTC"
      market mustEqual Market(Ltc, Btc)

      Market(Btc, Usd).getMarketSide() mustEqual Btc ~> Usd
      Market(Btc, Usd).getMarketSide(false) mustEqual Usd ~> Btc
    }

    "market side conversion" in {
      Btc ~> Usd mustEqual MarketSide(Btc, Usd)
      Btc ~> Usd mustEqual Btc ~> Usd
      Btc ~> Usd mustNotEqual Usd ~> Btc
      (Btc ~> Usd) == (Btc ~> Usd) mustEqual true
      (Btc ~> Usd).reverse mustEqual (Usd ~> Btc)
    }
  }
}