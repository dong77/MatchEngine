/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.bitway

import org.specs2.mutable._

import com.coinport.coinex.data._
import Implicits._
import Currency._

class AddressStatusSpec extends Specification {
  "AddressStatus" should {
    "could return amount by confirmation num" in {
      val addrStatus = AddressStatus(Some("123"), Some(2))
        .updateBook(Some(2), Some(8))
        .updateBook(Some(3), Some(-5))
        .updateBook(Some(3), Some(2))
        .updateBook(Some(4), Some(-9))
        .updateBook(Some(4), Some(7))
        .updateBook(Some(5), Some(-1))
        .updateBook(Some(6), Some(13))
        .updateBook(Some(7), Some(-4))
      addrStatus.getAddressStatusResult(Some(7)) mustEqual AddressStatusResult(Some("123"), Some(2), 11)
      addrStatus.getAddressStatusResult(Some(6)) mustEqual AddressStatusResult(Some("123"), Some(2), 15)
      addrStatus.getAddressStatusResult(Some(3)) mustEqual AddressStatusResult(Some("123"), Some(2), 5)
    }
  }
}
