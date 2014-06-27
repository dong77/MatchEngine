package com.coinport.coinex.common.assumption

import org.specs2.mutable._
import scala.collection.mutable.Map

// This test is not needed anymore, but we need to remember:
// "Construction of thrift state object should use `clone` to prevent being mutated.

/*
class ThriftStateImmutableAssumptionSpec extends Specification {
  "Construction of thrift state object" should {
    "use `clone` to prevent being mutated" in {
      val askMap = Map(1.0 -> 1L, 2.0 -> 2L)
      val bidMap = Map(1.0 -> 1L, 2.0 -> 2L)
      val state = TMarketDepthState(askMap, bidMap)

      askMap += (3.0 -> 3L)
      state.askMap.contains(3.0) mustEqual true
      state.askMap.size mustEqual 3

      val state2 = TMarketDepthState(askMap.clone, bidMap.clone)

      askMap += (4.0 -> 5L)
      state2.askMap.contains(4.0) mustEqual false
      state2.askMap.size mustEqual 3
    }
  }
}
*/
