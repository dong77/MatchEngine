/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import org.specs2.mutable._

class WindowQueueSpec extends Specification {
  "WindowQueue" should {
    "addAtTick" in {
      val wq = new WindowQueue[Int](10, 1)
      wq.addAtTick(5, 0) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAtTick(2, 6) mustEqual Array.empty[Int]
      wq.addAtTick(3, 7) mustEqual Array.empty[Int]
      wq.addAtTick(4, 9) mustEqual Array.empty[Int]
      wq.addAtTick(15, 11) mustEqual Array(5)
      wq.toList mustEqual List(0, 15, 0, 0, 0, 0, 2, 3, 0, 4)
      wq.addAtTick(17, 19) mustEqual Array(2, 3, 4)
      wq.toList mustEqual List(0, 15, 0, 0, 0, 0, 0, 0, 0, 17)
      wq.addAtTick(27, 29) mustEqual Array(15, 17)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 0, 0, 0, 0, 27)
      wq.addAtTick(37, 30) mustEqual Array.empty[Int]
      wq.toList mustEqual List(37, 0, 0, 0, 0, 0, 0, 0, 0, 27)
      wq.addAtTick(107, 105) mustEqual Array(27, 37)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 107, 0, 0, 0, 0)
      wq.addAtTick(14, 1007) mustEqual Array(107)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 0, 0, 14, 0, 0)
    }

    "addAfterTick" in {
      val wq = new WindowQueue[Int](10, 1)
      wq.addAfterTick(5, 1) mustEqual Array.empty[Int]
      wq.toList mustEqual List(0, 5, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAfterTick(2, 5) mustEqual Array.empty[Int]
      wq.addAfterTick(3, 1) mustEqual Array.empty[Int]
      wq.addAfterTick(4, 2) mustEqual Array.empty[Int]
      wq.addAfterTick(15, 2) mustEqual Array(5)
      wq.toList mustEqual List(0, 15, 0, 0, 0, 0, 2, 3, 0, 4)
      wq.addAfterTick(17, 8) mustEqual Array(2, 3, 4)
      wq.toList mustEqual List(0, 15, 0, 0, 0, 0, 0, 0, 0, 17)
      wq.addAfterTick(27, 10) mustEqual Array(15, 17)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 0, 0, 0, 0, 27)
      wq.addAfterTick(37, 1) mustEqual Array.empty[Int]
      wq.toList mustEqual List(37, 0, 0, 0, 0, 0, 0, 0, 0, 27)
      wq.addAfterTick(107, 75) mustEqual Array(27, 37)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 107, 0, 0, 0, 0)
      wq.addAfterTick(14, 902) mustEqual Array(107)
      wq.toList mustEqual List(0, 0, 0, 0, 0, 0, 0, 14, 0, 0)
    }
  }

  "WindowQueue" should {
    "addAtTick with 3 interval" in {
      val wq = new WindowQueue[Int](30, 3)
      wq.addAtTick(5, 1) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAtTick(4, 2) mustEqual null
      wq.toList mustEqual List(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAtTick(4, 5) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 4, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAtTick(30, 6) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 4, 30, 0, 0, 0, 0, 0, 0, 0)
      wq.addAtTick(32, 62) mustEqual Array(5, 4, 30)
      wq.toList mustEqual List(32, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    "addAfterTick with 3 interval" in {
      val wq = new WindowQueue[Int](30, 3)
      wq.addAfterTick(5, 1) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAfterTick(4, 1) mustEqual null
      wq.toList mustEqual List(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAfterTick(4, 3) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 4, 0, 0, 0, 0, 0, 0, 0, 0)
      wq.addAfterTick(30, 1) mustEqual Array.empty[Int]
      wq.toList mustEqual List(5, 4, 30, 0, 0, 0, 0, 0, 0, 0)
      wq.addAfterTick(32, 56) mustEqual Array(5, 4, 30)
      wq.toList mustEqual List(32, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
  }
}
