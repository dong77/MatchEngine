package com.coinport.coinex.util

import com.twitter.chill.ScalaKryoInstantiator

object ScalaSerializer {
  def serialize[T](t: T): Array[Byte] = ScalaKryoInstantiator.defaultPool.toBytesWithClass(t)
  def deserialize[T](bytes: Array[Byte]): T =
    ScalaKryoInstantiator.defaultPool.fromBytes(bytes).asInstanceOf[T]
}