/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.metrics

import org.specs2.mutable._

class StackQueueSpec extends Specification {
  "StackQueue" should {
    "test random push and dequeue" in {
      val sq = new StackQueue[Int]((lhs: Int, rhs: Int) => lhs <= rhs)
      sq.push(2)
      sq.toList mustEqual List(2)
      sq.front.get mustEqual 2
      sq.push(7)
      sq.toList mustEqual List(2, 7)
      sq.front.get mustEqual 2
      sq.push(3)
      sq.toList mustEqual List(2, 3)
      sq.front.get mustEqual 2
      sq.push(1)
      sq.toList mustEqual List(1)
      sq.front.get mustEqual 1
      sq.push(4)
      sq.toList mustEqual List(1, 4)
      sq.front.get mustEqual 1
      sq.push(10)
      sq.toList mustEqual List(1, 4, 10)
      sq.front.get mustEqual 1
      sq.push(9)
      sq.toList mustEqual List(1, 4, 9)
      sq.front.get mustEqual 1
      sq.push(8)
      sq.toList mustEqual List(1, 4, 8)
      sq.front.get mustEqual 1
      sq.dequeue(7)
      sq.toList mustEqual List(1, 4, 8)
      sq.front.get mustEqual 1
      sq.dequeue(1)
      sq.toList mustEqual List(4, 8)
      sq.front.get mustEqual 4
      sq.dequeue(3)
      sq.toList mustEqual List(4, 8)
      sq.front.get mustEqual 4
      sq.dequeue(4)
      sq.toList mustEqual List(8)
      sq.front.get mustEqual 8
      sq.push(8)
      sq.push(8)
      sq.push(8)
      sq.toList mustEqual List(8, 8, 8, 8)
      sq.front.get mustEqual 8
      sq.dequeue(8)
      sq.toList mustEqual List(8, 8, 8)
      sq.front.get mustEqual 8
      sq.dequeue(8)
      sq.dequeue(8)
      sq.dequeue(8)
      sq.toList mustEqual List()
      sq.dequeue(8)
      sq.dequeue(8)
      sq.dequeue(8)
      sq.front mustEqual None
      sq.push(2)
      sq.push(8)
      sq.push(8)
      sq.push(8)
      sq.toList mustEqual List(2, 8, 8, 8)
      sq.front mustEqual Some(2)
      sq.push(1)
      sq.toList mustEqual List(1)
      sq.front mustEqual Some(1)
    }
    "test ascent push order" in {
      val sq = new StackQueue[Int]((lhs: Int, rhs: Int) => lhs <= rhs)
      sq.push(1)
      sq.push(2)
      sq.push(3)
      sq.push(4)
      sq.push(5)
      sq.push(6)
      sq.push(7)
      sq.push(8)
      sq.push(9)
      sq.toList mustEqual List(1, 2, 3, 4, 5, 6, 7, 8, 9)
      sq.dequeue(1)
      sq.dequeue(2)
      sq.dequeue(3)
      sq.dequeue(4)
      sq.dequeue(5)
      sq.dequeue(6)
      sq.dequeue(7)
      sq.dequeue(8)
      sq.dequeue(9)
      sq.dequeue(10)
      sq.front mustEqual None
      sq.toList mustEqual List()
    }
  }
}
