package com.coinport.coinex.serializers

import org.specs2.mutable._
import org.json4s.JsonDSL._
import org.json4s.native.Serialization._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.Currency._
import org.json4s.native.Serialization
import org.json4s._

case class CurrencyWrapper(currency: Currency)
case class MarketSideWrapper(side: MarketSide)
case class RedeliverFiltersWrapper(filters: RedeliverFilters)

class PrettyJsonSerializerTest extends Specification {
  implicit val formats = Serialization.formats(NoTypeHints)
  "Any to JSON" should {
    "Currency to JSON" in {
      val json = ("currency" -> "BTC")
      PrettyJsonSerializer.toJson(CurrencyWrapper(Btc)) mustEqual writePretty(json)
    }

    "MarketSide to JSON" in {
      val json = ("side" -> "BTC-USD")
      PrettyJsonSerializer.toJson(MarketSideWrapper(Btc ~> Usd)) mustEqual writePretty(json)
    }

    "immutable map to JSON" in {
      val map = scala.collection.immutable.Map(
        Btc ~> Usd -> 456.7,
        Ltc ~> Usd -> 12.3
      )

      val json =
        ("BTC-USD" -> 456.7) ~
          ("LTC-USD" -> 12.3)

      PrettyJsonSerializer.toJson(map) mustEqual writePretty(json)
    }

    "mutable map to JSON" in {
      val map = scala.collection.mutable.Map(
        Btc ~> Usd -> 456.7,
        Ltc ~> Usd -> 12.3
      )

      val json =
        ("BTC-USD" -> 456.7) ~
          ("LTC-USD" -> 12.3)

      PrettyJsonSerializer.toJson(map) mustEqual writePretty(json)
    }

    "nested structure to JSON" in {
      val list1 = List(Order(11L, 123L, 456), Order(12L, 123L, 457))
      val list2 = List(Order(21L, 123L, 111), Order(22L, 123L, 222))
      val pools = scala.collection.immutable.Map(
        Btc ~> Usd -> list1,
        Ltc ~> Usd -> list2
      )

      val mapper = {
        o: Order =>
          ("userId" -> o.userId) ~ ("id" -> o.id) ~ ("quantity" -> o.quantity) ~ ("inAmount" -> o.inAmount)
      }

      val json =
        ("BTC-USD" -> list1.map(mapper)) ~
          ("LTC-USD" -> list2.map(mapper))

      PrettyJsonSerializer.toJson(pools) mustEqual writePretty(json)
    }

    "redeliverFilters filtered" in {
      val filters = RedeliverFilters(Map("rfd" -> RedeliverFilterData(List(100, 200), 20)))

      val prettyJson = ("filters" -> ("filterMap" -> ("rfd" -> (("processedIds" -> List(100, 200)) ~ ("maxSize" -> 20)))))

      val openJson = (JNothing -> JNothing)

      PrettyJsonSerializer.toJson(RedeliverFiltersWrapper(filters)) mustEqual writePretty(prettyJson)
      OpenDataJsonSerializer.toJson(RedeliverFiltersWrapper(filters)) mustEqual writePretty(openJson)
    }
  }
}
