/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import scala.collection.mutable.ArrayBuffer

import com.coinport.coinex.data._

class StackQueue[T](elems: ArrayBuffer[T], ordering: (T, T) => Boolean)(implicit m: Manifest[T]) extends Serializable {

  def this(ordering: (T, T) => Boolean)(implicit m: Manifest[T]) = this(
    new ArrayBuffer[T](), ordering)

  def push(elem: T): StackQueue[T] = {
    val lastIndex = lastIndexWhere((e: T) => ordering(e, elem)) + 1
    if (lastIndex == 0) {
      elems.clear()
    } else {
      elems.remove(lastIndex, elems.length - lastIndex)
    }
    elems += elem
    this
  }

  def dequeue(elem: T): StackQueue[T] = {
    front match {
      case Some(f) if (f == elem) =>
        elems.remove(0, 1)
      case _ => None
    }
    this
  }

  def front: Option[T] = if (elems.length == 0) None else Some(elems(0))

  def copy = new StackQueue[T](elems, ordering)

  def toList = elems.toList

  def size = elems.size

  override def toString() = "StackQueue%s".format(toList).replace("List", "")

  private def lastIndexWhere(predict: (T) => Boolean): Int = {
    for (i <- elems.length - 1 to 0 by -1)
      if (predict(elems(i)))
        return i
    return -1
  }
}
