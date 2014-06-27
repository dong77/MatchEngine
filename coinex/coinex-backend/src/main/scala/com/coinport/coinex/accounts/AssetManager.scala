package com.coinport.coinex.accounts

import com.coinport.coinex.common.Manager
import scala.collection.mutable.Map
import com.coinport.coinex.data._

class AssetManager extends Manager[TAssetState] {
  private val currencyMap = Map.empty[Currency, Long]
  private val timeAsset = Map.empty[Long, Map[Currency, Long]]
  private var historyAssetMap = Map.empty[Long, Map[Long, Map[Currency, Long]]]
  private var currentAssetMap = Map.empty[Long, Map[Currency, Long]]

  private val timePrice = Map.empty[Long, Double]
  private var historyPriceMap = Map.empty[MarketSide, Map[Long, Double]]
  private var currentPriceMap = Map.empty[MarketSide, Double]

  val day = 1000 * 60 * 60 * 24
  //  val day = 1000 * 60

  // Thrift conversions     ----------------------------------------------
  def getSnapshot = TAssetState(currentAssetMap, historyAssetMap, currentPriceMap, historyPriceMap)

  def loadSnapshot(snapshot: TAssetState) = {
    currentAssetMap = currentAssetMap.empty ++ snapshot.currentAssetMap.map(
      x => x._1 -> (currencyMap.empty ++ x._2))

    historyAssetMap = historyAssetMap.empty ++ snapshot.historyAssetMap.map(
      x => x._1 -> (timeAsset.empty ++ x._2.map(
        y => y._1 -> (currencyMap.empty ++ y._2))))

    currentPriceMap = currentPriceMap.empty ++ snapshot.currentPriceMap

    historyPriceMap = historyPriceMap.empty ++ snapshot.historyPriceMap.map(
      x => x._1 -> (timePrice.empty ++ x._2))
  }

  def updateAsset(user: Long, timestamp: Long, currency: Currency, volume: Long) = {
    val timeDay = timestamp / day
    historyAssetMap.get(user) match {
      case Some(t) => t.get(timeDay) match {
        case Some(ua) => ua.put(currency, ua.getOrElse(currency, 0L) + volume)
        case None => t.put(timeDay, Map(currency -> volume))
      }
      case None => historyAssetMap.put(user, Map(timeDay -> Map(currency -> volume)))
    }

    currentAssetMap.get(user) match {
      case Some(curMap) => curMap.get(currency) match {
        case Some(old) => curMap.put(currency, old + volume)
        case None => curMap.put(currency, volume)
      }
      case None => currentAssetMap.put(user, Map(currency -> volume))
    }
  }

  def updatePrice(side: MarketSide, timestamp: Long, price: Double) = {
    val timeDay = timestamp / day
    historyPriceMap.get(side) match {
      case Some(pm) => pm.put(timeDay, price)
      case None => historyPriceMap.put(side, Map(timeDay -> price))
    }

    currentPriceMap.put(side, price)
  }

  def getHistoryAsset(userId: Long, from: Long, to: Long) = {
    historyAssetMap.get(userId) match {
      case Some(timeAsset) =>
        (from / day to to / day).map { i =>
          val x = timeAsset.get(i).map(i -> _)
          x
        }.filter(_.isDefined).map(_.get).toMap
      case None => Map[Long, Map[Currency, Long]]().toMap
    }
  }

  def getCurrentAsset(userId: Long) =
    currentAssetMap.get(userId).getOrElse(Map.empty[Currency, Long])

  def getHistoryPrice(from: Long, to: Long) =
    historyPriceMap.map {
      case (side, timePrice) =>
        side -> (from / day to to / day).map(i => timePrice.get(i).map(i -> _)).filter(_.isDefined).map(_.get).toMap
    }

  def getCurrentPrice = currentPriceMap
}
