package com.coinport.coinex.serializers

import com.twitter.scrooge._
import com.twitter.bijection.scrooge.ScalaCodec

object JsonScalaCodec {
  def apply[T <: ThriftStruct](c: ThriftStructCodec[T]) =
    new JsonScalaCodec[T](c)
}

class JsonScalaCodec[T <: ThriftStruct](c: ThriftStructCodec[T])
  extends ScalaCodec(new JsonThriftSerializer[T] {
    override def codec = c
  })