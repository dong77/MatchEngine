/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.common

import com.coinport.coinex.data._

final class SimpleManager extends Manager[TSimpleState] {
  var state = TSimpleState(RedeliverFilters(Map.empty[String, RedeliverFilterData]))

  override def getSnapshot = state.copy(filters = getFiltersSnapshot)

  override def loadSnapshot(s: TSimpleState) {
    state = s
    loadFiltersSnapshot(s.filters)
  }
}
