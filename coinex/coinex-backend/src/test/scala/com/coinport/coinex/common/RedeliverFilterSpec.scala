package com.coinport.coinex.common

import com.coinport.coinex.data.RedeliverFilterData
import org.specs2.mutable._

class RedeliverFilterSpec extends Specification {
  "RedeliverFilter" should {
    "restore state and restrict sizes" in {
      val state = RedeliverFilterData(Seq(10, 2, 4, 6, 19), 8)
      val filter = new RedeliverFilter(state)
      filter.processedIds.toSeq mustEqual Seq(2, 4, 6, 10, 19)

      var processed = 0
      filter.filter(0) { _ => failure("0 should be filtered out") }
      filter.rememberProcessedId(0)
      filter.filter(1) { _ => failure("1 should be filtered out") }
      filter.rememberProcessedId(1)
      filter.filter(2) { _ => failure("1 should be filtered out") }
      filter.rememberProcessedId(2)
      filter.filter(3) { _ => processed += 1 }
      filter.rememberProcessedId(3)
      filter.filter(4) { _ => failure("1 should be filtered out") }
      filter.rememberProcessedId(4)
      filter.filter(5) { _ => processed += 1 }
      filter.rememberProcessedId(5)
      filter.filter(6) { _ => failure("1 should be filtered out") }
      filter.rememberProcessedId(6)
      filter.filter(19) { _ => failure("1 should be filtered out") }
      filter.rememberProcessedId(19)
      filter.filter(20) { _ => processed += 1 }
      filter.rememberProcessedId(20)
      filter.filter(100) { _ => processed += 1 }
      filter.rememberProcessedId(100)
      filter.filter(101) { _ => processed += 1 }
      filter.rememberProcessedId(101)
      processed mustEqual 5
      filter.processedIds.toSeq mustEqual Seq(4, 5, 6, 10, 19, 20, 100, 101)
    }
  }
}
