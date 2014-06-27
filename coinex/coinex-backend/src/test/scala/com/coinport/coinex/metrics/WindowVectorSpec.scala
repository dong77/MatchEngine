/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import org.specs2.mutable._

class WindowVectorSpec extends Specification {
  "WindowVector" should {
    "addAtTick" in {
      val wq = new WindowVector[Int](10)
      wq.addAtTick(5, 0) mustEqual Seq.empty[Int]
      wq.toTList mustEqual List(5)
      wq.addAtTick(2, 6) mustEqual Seq.empty[Int]
      wq.addAtTick(3, 7) mustEqual Seq.empty[Int]
      wq.addAtTick(4, 9) mustEqual Seq.empty[Int]
      wq.addAtTick(15, 11) mustEqual Seq(5)
      wq.toTList mustEqual List(2, 3, 4, 15)
      wq.addAtTick(17, 19) mustEqual Seq(2, 3, 4)
      wq.toTList mustEqual List(15, 17)
      wq.addAtTick(27, 29) mustEqual Seq(15, 17)
      wq.toTList mustEqual List(27)
      wq.addAtTick(37, 30) mustEqual Seq.empty[Int]
      wq.toTList mustEqual List(27, 37)
      wq.addAtTick(107, 105) mustEqual Seq(27, 37)
      wq.toTList mustEqual List(107)
      wq.addAtTick(14, 1007) mustEqual Seq(107)
      wq.toTList mustEqual List(14)
    }
  }
}
