package com.coinport.coinex.accounts

import org.specs2.mutable.Specification
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.ChartTimeDimension
import com.coinport.coinex.api.model.timeDimension2MilliSeconds

class AssetManagerSpec extends Specification {
  "UserAssetSpec" should {
    val day = 1397486565673L
    val day2 = day + 1000 * 3600 * 24
    val day3 = day2 + 1000 * 3600 * 24

    val timeSkip = ChartTimeDimension.OneDay
    val key = day / timeSkip
    val key2 = day2 / timeSkip
    val key3 = day3 / timeSkip
    "update user asset and can get them all" in {
      val manager = new AssetManager()

      manager.updateAsset(1L, day, Cny, 1000)
      manager.updateAsset(1L, day, Btc, 1000)

      manager.updateAsset(1L, day2, Pts, 1000)
      manager.updateAsset(1L, day2, Ltc, 1000)
      manager.updateAsset(1L, day2, Cny, 1000)

      manager.updateAsset(1L, day3, Pts, -500)
      manager.updateAsset(1L, day3, Cny, -500)

      //            println(manager.getHistoryAsset(1L, 0, day2))
      manager.getHistoryAsset(1L, 0, day2) mustEqual
        //        Map(16174 -> Map(Btc -> 1000, Cny -> 1000), 16175 -> Map(Cny -> 1000, Ltc -> 1000, Pts -> 1000))
        Map(key -> Map(Btc -> 1000, Cny -> 1000), key2 -> Map(Cny -> 1000, Ltc -> 1000, Pts -> 1000))

      manager.getCurrentAsset(1L) mustEqual
        Map(Cny -> 1500, Ltc -> 1000, Btc -> 1000, Pts -> 500)
    }

    "update price of currency and can get them all" in {
      val manager = new AssetManager()

      manager.updatePrice(Btc ~> Cny, day, 3000)
      manager.updatePrice(Ltc ~> Cny, day2, 200)
      manager.updatePrice(Pts ~> Cny, day3, 100)
      manager.updatePrice(Btc ~> Cny, day3, 4000)

      //      println(manager.getHistoryPrice(0, day3))
      manager.getHistoryPrice(0, day3) mustEqual
        Map(Pts ~> Cny -> Map(key3 -> 100.0), Btc ~> Cny -> Map(key -> 3000.0, key3 -> 4000.0), Ltc ~> Cny -> Map(key2 -> 200.0))
      //        Map(Btc ~> Cny -> Map(16174 -> 3000, 16176 -> 4000), Ltc ~> Cny -> Map(16175 -> 200), Pts ~> Cny -> Map(16176 -> 100))
      manager.getCurrentPrice mustEqual Map(Btc ~> Cny -> 4000, Ltc ~> Cny -> 200, Pts ~> Cny -> 100)
    }
  }
}
