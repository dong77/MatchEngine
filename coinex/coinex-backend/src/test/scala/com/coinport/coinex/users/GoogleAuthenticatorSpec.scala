/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 */

package com.coinport.coinex.users

import org.specs2.mutable._
import org.apache.commons.codec.binary.Base32

class GoogleAuthenticatorSpec extends Specification {
  val auth = new GoogleAuthenticator

  "GoogleAuthenticator" should {

    "authenticate code correctly with 1 variance" in {
      val secret = auth.createSecret
      val secretBytes = new Base32().decode(secret)

      val millis = System.currentTimeMillis()
      val timeIndex = auth.getTimeIndex(millis)

      val code = auth.getCode(secretBytes, timeIndex)
      auth.verifyCode(secret, code, timeIndex, 1) mustEqual true
    }

    "authenticate code correctly with 2 variance" in {
      val secret = auth.createSecret
      val secretBytes = new Base32().decode(secret)

      val millis = System.currentTimeMillis()
      val timeIndex = auth.getTimeIndex(millis)

      val code = auth.getCode(secretBytes, timeIndex)
      auth.verifyCode(secret, code, timeIndex + 3, 3) mustEqual true
      auth.verifyCode(secret, code, timeIndex - 3, 3) mustEqual true
    }

    "not authenticate code if time index is out of variance" in {
      val secret = auth.createSecret
      val secretBytes = new Base32().decode(secret)

      val millis = System.currentTimeMillis()
      val timeIndex = auth.getTimeIndex(millis)

      val code = auth.getCode(secretBytes, timeIndex)
      auth.verifyCode(secret, code, timeIndex + 3, 2) mustEqual false
      auth.verifyCode(secret, code, timeIndex - 3, 2) mustEqual false
    }
  }
}