/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import scala.collection.mutable.ArrayBuffer

import com.coinport.coinex.data._

class WindowVector[T](val range: Long, val elems: ArrayBuffer[(T, Long)])(implicit m: Manifest[T])
    extends Serializable {

  val NULL: T = new Array[T](1)(0)

  def this(range: Long)(implicit m: Manifest[T]) = this(range, new ArrayBuffer[(T, Long)]())

  def addAtTick(elem: T, tick: Long): Seq[T] = {
    if (elem != NULL) elems += ((elem, tick))
    val threshold = tick - range
    val firstIndex = elems.indexWhere((t) => t._2 > threshold)
    val removedElems = if (firstIndex < 0) {
      val ret = elems.map(_._1)
      elems.clear()
      ret
    } else {
      val ret = elems.slice(0, firstIndex).map(_._1)
      elems.remove(0, firstIndex)
      ret
    }
    removedElems
  }

  def copy = new WindowVector[T](range, elems.clone)

  def toList = elems.toList

  private[metrics] def toTList = elems.map(_._1)

  override def toString() = "WindowVector(elems: %s)".format(toList).replace("List", "")
}
