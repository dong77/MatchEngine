/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.service

import com.coinport.coinex.data._
import com.coinport.coinex.api.model._
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.data.Implicits._

object MarketService extends AkkaService {
  def getDepth(marketSide: MarketSide, depth: Int): Future[ApiResult] = {
    backend ? QueryMarketDepth(marketSide, depth) map {
      case result: QueryMarketDepthResult => ApiResult(data = Some(fromMarketDepth(result.marketDepth)))
      case x => ApiResult(false)
    }
  }

  def getHistory(marketSide: MarketSide, timeDimension: ChartTimeDimension, from: Long, to: Long): Future[ApiResult] = {
    backend ? QueryCandleData(marketSide, timeDimension, from, to) map {
      case rv: QueryCandleDataResult =>
        val (candles, side, timeSkip, currency) = (rv.candleData, rv.candleData.side, timeDimension, rv.candleData.side.outCurrency)
        val candleSeq = candles.items.map(item => fromCandleItem(item, side, currency, timeSkip))
        ApiResult(data = Some(ApiHistory(candleSeq)))
      case x =>
        ApiResult(false)
    }
  }

  def getTransactions(marketSide: Option[MarketSide], tid: Option[Long], uid: Option[Long], orderId: Option[Long], skip: Int, limit: Int): Future[ApiResult] = {
    val cursor = Cursor(skip, limit)
    val queryMarketSide = marketSide.map(ms => QueryMarketSide(ms, true))
    backend ? QueryTransaction(tid, uid, orderId, queryMarketSide, cursor) map {
      case result: QueryTransactionResult =>
        val items = result.transactions map (t => fromTransaction(t))
        ApiResult(data = Some(ApiPagingWrapper(skip, limit, items, result.count.toInt)))
    }
  }

  def getGlobalTransactions(marketSide: Option[MarketSide], skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, None, None, skip, limit)

  def getTransactionsByUser(marketSide: Option[MarketSide], uid: Long, skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, Some(uid), None, skip, limit)

  def getTransactionsByOrder(marketSide: Option[MarketSide], orderId: Long, skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, None, Some(orderId), skip, limit)

  def getTransaction(tid: Long) = {
    backend ? QueryTransaction(Some(tid), None, None, None, Cursor(0, 1)) map {
      case result: QueryTransactionResult =>
        val transaction = result.transactions(0)
        val data = fromTransaction(transaction)
        ApiResult(data = Some(data))
    }
  }

  def getAsset(userId: Long, from: Long, to: Long, baseCurrency: Currency) = {
    backend ? QueryAsset(userId, from, to) map {
      case result: QueryAssetResult =>
        val timeSkip: Long = ChartTimeDimension.OneDay
        val start = Math.min(from / timeSkip, to / timeSkip)
        val stop = Math.max(from / timeSkip, to / timeSkip)

        val historyAsset = result.historyAsset
        val historyPrice = result.historyPrice

        val currentPrice = result.currentPrice.priceMap
        val currencyPriceMap = scala.collection.mutable.Map.empty[Currency, Map[Long, Double]]
        historyPrice.priceMap.foreach {
          case (side, map) =>
            if (side._2 == baseCurrency) {
              var curPrice = currentPrice.get(side).get

              val priceMap = (start to stop).reverse.map { timeSpot =>
                curPrice = map.get(timeSpot).getOrElse(curPrice)
                timeSpot -> curPrice.externalValue(side)
              }.toMap
              currencyPriceMap.put(side._1, priceMap)
            }
        }

        val currentAsset = scala.collection.mutable.Map.empty[Currency, Long] ++ result.currentAsset.currentAsset
        val assetList = (start to stop).reverse.map { timeSpot =>
          val rv = (timeSpot, currentAsset.clone())

          historyAsset.currencyMap.get(timeSpot) match {
            case Some(curMap) =>
              curMap.foreach {
                case (cur, volume) =>
                  currentAsset.put(cur, currentAsset.get(cur).get - volume)
              }
            case None =>
          }
          rv
        }

        val items = assetList.map {
          case (timeSpot, assetMap) =>
            val amountMap = assetMap.map {
              case (cur, volume) =>
                val (amount, price) =
                  if (cur == baseCurrency) (volume, 1.0)
                  else currencyPriceMap.get(cur) match {
                    case Some(curHisMap) =>
                      val price = curHisMap.get(timeSpot).get
                      ((BigDecimal(price) * BigDecimal(volume)).longValue(), price)
                    case None => (0L, 0.0)
                  }
                cur.toString.toUpperCase -> (CurrencyObject(cur, amount), PriceObject(cur ~> baseCurrency, price))
            }.toMap
            ApiAssetItem(uid = userId.toString,
              assetMap = assetMap.map(a => a._1.toString.toUpperCase -> CurrencyObject(a._1, a._2)).toMap,
              amountMap = amountMap.map(a => a._1 -> a._2._1),
              priceMap = amountMap.map(a => a._1 -> a._2._2),
              timestamp = timeSpot * timeSkip)
        }
        ApiResult(data = Some(items.reverse))
    }
  }

  def getTickers(marketSides: Seq[MarketSide]) = {
    backend ? QueryMetrics map {
      case result: Metrics =>
        val map = result.metricsByMarket
        val data = marketSides
          .filter(s => map.contains(s))
          .map(side => fromTicker(map.get(side).get, side, side.outCurrency))
        ApiResult(data = Some(data))
    }
  }
}
