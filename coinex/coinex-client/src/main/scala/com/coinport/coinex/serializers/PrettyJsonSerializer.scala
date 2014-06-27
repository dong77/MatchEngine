/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 */

package com.coinport.coinex.serializers

import org.json4s._
import org.json4s.JsonAST.JField
import org.json4s.native.Serialization._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._

object PrettyJsonSerializer extends BaseJsonSerializer {
  implicit val formats = ThriftEnumJson4sSerialization.formats + new DebugTypeSerializer
}

object OpenDataJsonSerializer extends BaseJsonSerializer {
  implicit val formats = ThriftEnumJson4sSerialization.formats + new CustomTypeSerializer
}

trait BaseJsonSerializer {
  implicit val formats: Formats

  def toJValue(obj: Any) = {
    Extraction.decompose(obj)
      .removeField {
        case JField(name, value) =>
          name.startsWith("_") // filter fields starting with underscore
        case _ => false
      }
  }

  def toJson(obj: Any): String = {
    writePretty(toJValue(obj))
  }
}

class CustomTypeSerializer extends DebugTypeSerializer {
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] =
    super.serialize orElse {
      case m: RedeliverFilters => JNothing
    }
}

class DebugTypeSerializer extends Serializer[Any] {
  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case m: Currency => JString(m.toString.toUpperCase)
    case m: MarketSide => JString(m.S)
    case m: scala.collection.Map[_, _] =>
      JObject(m.map({
        case (k, v) =>
          JField(
            k match {
              case ks: String => ks
              case ks: MarketSide => ks.S
              case ks: Any => ks.toString
            },
            Extraction.decompose(v))
      }).toList)
  }

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Map[Any, Any]] = {
    sys.error("Not interested.")
  }
}