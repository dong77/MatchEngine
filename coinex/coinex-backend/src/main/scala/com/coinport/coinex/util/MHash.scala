/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.util

import java.security.MessageDigest

import com.google.common.io.BaseEncoding

object MHash {
  def sha256Base64(str: String) = BaseEncoding.base64.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha256Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha1Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-1").digest(str.getBytes("UTF-8")))
  def murmur3(str: String): Long = MurmurHash3.MurmurHash3_x64_64(str.getBytes, 100416)
  def sha256ThenMurmur3(text: String): Long = MHash.murmur3(sha256Base64(text))
}
