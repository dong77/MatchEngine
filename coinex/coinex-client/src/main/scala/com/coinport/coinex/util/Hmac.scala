/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.util

import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import com.google.common.io.BaseEncoding

object Hash {
  def hmacSha1Base64(text: String, secret: String): String = {
    val signingKey = new SecretKeySpec(secret.getBytes, "HmacSHA1");
    val mac = Mac.getInstance("HmacSHA1");
    mac.init(signingKey);
    val bytes = mac.doFinal(text.getBytes)
    BaseEncoding.base64.encode(bytes)
  }
}