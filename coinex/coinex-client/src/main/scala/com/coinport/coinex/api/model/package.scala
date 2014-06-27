/**
 * Copyright (C) 2014 Coinport Inc.
 */
package com.coinport.coinex.api

import com.coinport.coinex.data.ChartTimeDimension._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data._
import org.json4s.ext.EnumNameSerializer
import org.json4s.JsonAST.JField
import org.json4s.native.Serialization
import org.json4s._
import scala.concurrent.duration._
import scala.Some

package object model {
  implicit def long2CurrencyWrapper(value: Long) = new CurrencyWrapper(value)

  implicit def double2CurrencyWrapper(value: Double) = new CurrencyWrapper(value)

  implicit def double2PriceWrapper(value: Double): PriceWrapper = new PriceWrapper(value)

  implicit def priceWrapper2Double(value: PriceWrapper) = value

  implicit def string2Currency(currencyString: String): Currency = {
    Currency.valueOf(currencyString.toLowerCase.capitalize).getOrElse(Unknown)
  }

  implicit def currency2String(currency: Currency): String = {
    currency.name.toUpperCase
  }

  // candle data conversions
  implicit def timeDimension2MilliSeconds(dimension: ChartTimeDimension): Long = {
    val duration = dimension match {
      case OneMinute => 1 minute
      case ThreeMinutes => 3 minutes
      case FiveMinutes => 5 minutes
      case FifteenMinutes => 15 minutes
      case ThirtyMinutes => 30 minutes
      case OneHour => 1 hour
      case TwoHours => 2 hours
      case FourHours => 4 hours
      case SixHours => 6 hours
      case TwelveHours => 12 hours
      case OneDay => 1 day
      case ThreeDays => 3 days
      case OneWeek => 7 days
    }
    duration.toMillis
  }

  class CandleDataItemSerializer() extends CustomSerializer[ApiCandleItem](
    format => ({
      null // deserializer is not implemented
    }, {
      case item: ApiCandleItem =>
        JArray(List(
          JDecimal(item.time),
          JDouble(item.open.value),
          JDouble(item.high.value),
          JDouble(item.low.value),
          JDouble(item.close.value),
          JDouble(item.outAmount.value)
        ))
    })
  )

  implicit val formats = Serialization.formats(NoTypeHints) + new EnumNameSerializer(Operations) + new CandleDataItemSerializer()

  class JsonSupportWrapper(obj: Any) {
    def toJson(): JValue = {
      val json = Extraction.decompose(obj)
      json filterField {
        case JField(name, value) =>
          !name.startsWith("_") // filter fields starting with underscore
        case _ => false
      }
      json
    }
  }

  implicit def toJsonSupportWrapper(obj: Any): JsonSupportWrapper = new JsonSupportWrapper(obj)

  def fromUserAccount(account: UserAccount): ApiUserAccount = {
    val userId = account.userId.toString
    val accounts = account.cashAccounts.map {
      case (k: Currency, v: CashAccount) =>
        val currency: String = k
        currency -> ApiAccountItem(
          currency,
          CurrencyObject(k, v.available),
          CurrencyObject(k, v.locked),
          CurrencyObject(k, v.pendingWithdrawal),
          CurrencyObject(k, v.available + v.locked + v.pendingWithdrawal)
        )
    }.toMap
    ApiUserAccount(userId, accounts)
  }

  def fromTransaction(t: Transaction) = {
    val marketSide = t.side.normalized
    val sell = t.side.ordered

    val (tOrder, mOrder, price, subjectAmount, currencyAmount) = {
      val subject = marketSide.outCurrency
      val currency = marketSide.inCurrency

      val takerAmount = t.takerUpdate.previous.quantity - t.takerUpdate.current.quantity
      val makerAmount = t.makerUpdate.previous.quantity - t.makerUpdate.current.quantity
      val (sAmount, cAmount) = if (sell) (takerAmount, makerAmount) else (makerAmount, takerAmount)
      val (takerPreAmount, takeCurAmount, makerPreAmount, makerCurAmount) =
        if (sell)
          (CurrencyObject(subject, t.takerUpdate.previous.quantity),
            CurrencyObject(subject, t.takerUpdate.current.quantity),
            CurrencyObject(currency, t.makerUpdate.previous.quantity),
            CurrencyObject(currency, t.makerUpdate.current.quantity))
        else
          (CurrencyObject(currency, t.takerUpdate.previous.quantity),
            CurrencyObject(currency, t.takerUpdate.current.quantity),
            CurrencyObject(subject, t.makerUpdate.previous.quantity),
            CurrencyObject(subject, t.makerUpdate.current.quantity))

      (ApiOrderState(t.takerUpdate.current.id.toString, t.takerUpdate.current.userId.toString, takerPreAmount, takeCurAmount),
        ApiOrderState(t.makerUpdate.current.id.toString, t.makerUpdate.current.userId.toString, makerPreAmount, makerCurAmount),
        PriceObject(marketSide, cAmount.toDouble / sAmount.toDouble),
        CurrencyObject(subject, sAmount),
        CurrencyObject(currency, cAmount))
    }

    val id = t.id.toString
    val timestamp = t.timestamp
    val taker = tOrder.uid
    val maker = mOrder.uid
    ApiTransaction(id, timestamp, price, subjectAmount, currencyAmount, maker, taker, sell, tOrder, mOrder)
  }

  def fromTicker(metrics: MetricsByMarket, side: MarketSide, currency: Currency) = {
    val market = side.S
    val price = PriceObject(side, metrics.price)
    val volume = CurrencyObject(currency, metrics.volume)
    val high = PriceObject(side, metrics.high.getOrElse(0.0))
    val low = PriceObject(side, metrics.low.getOrElse(0.0))
    val gain = metrics.gain
    val trend = Some(metrics.direction.toString.toLowerCase)

    ApiTicker(market, price, high, low, volume, gain, trend)
  }

  def fromTransferItem(t: AccountTransfer) = {
    val id = t.id.toString
    val uid = t.userId.toString
    val amount = CurrencyObject(t.currency, t.amount)
    val status = t.status.value
    val created = t.created.getOrElse(0L)
    val updated = t.updated.getOrElse(0L)
    val operation = t.`type`.getValue
    val txid = t.txid.getOrElse("")

    ApiTransferItem(id, uid, amount, status, created, updated, operation, t.address.getOrElse(""), txid)
  }

  def fromMarketDepth(depth: MarketDepth) = {
    val bids = depth.bids.map(item => ApiMarketDepthItem(PriceObject(depth.side, item.price), CurrencyObject(depth.side.outCurrency, item.quantity)))
    val asks = depth.asks.map(item => ApiMarketDepthItem(PriceObject(depth.side, item.price), CurrencyObject(depth.side.outCurrency, item.quantity)))

    ApiMarketDepth(bids, asks)
  }

  def fromCandleItem(item: CandleDataItem, side: MarketSide, currency: Currency, timeSkip: Long) = {
    val time = item.timestamp * timeSkip
    val open = PriceObject(side, item.open)
    val high = PriceObject(side, item.high)
    val low = PriceObject(side, item.low)
    val close = PriceObject(side, item.close)
    val outAmount = CurrencyObject(currency, item.outAoumt)

    ApiCandleItem(time, open, high, low, close, outAmount)
  }

  def fromProfile(u: UserProfile) = {
    val dmap = u.depositAddresses.map(_.toMap)
    val wmap = u.depositAddresses.map(_.toMap)
    User(u.id, u.email, u.realName, u.passwordHash.getOrElse(""), u.nationalId, u.mobile, dmap, wmap)
  }

  def fromNotification(n: Notification) = {
    ApiNotification(n.id, n.nType.toString, n.title, n.content, n.created, n.updated, n.removed, n.lang.toString)
  }
}
