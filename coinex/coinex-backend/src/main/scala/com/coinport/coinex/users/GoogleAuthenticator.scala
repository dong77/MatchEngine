package com.coinport.coinex.users

import java.security._
import org.apache.commons.codec.binary.Base32
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import scala.collection.mutable.ArrayBuffer
import java.nio.ByteBuffer

// https://github.com/evanx/vellum/blob/c1329e5a97cf21ff1a8a186e961d6397b7e96176/src/vellumdemo/totp/GenerateGoogleTotpQrUrl.java

final class GoogleAuthenticator {
  private val rand = SecureRandom.getInstance("SHA1PRNG", "SUN")

  def createSecret = {
    val buffer = new Array[Byte](10)
    rand.nextBytes(buffer)
    new String(new Base32().encode(buffer));
  }

  def getTimeIndex(millis: Long = System.currentTimeMillis) = millis / 30000

  def verifyCode(secret: String, code: Int, timeIndex: Long, variance: Int): Boolean = {
    val secretBytes = new Base32().decode(secret)
    (-variance to variance) map { i =>
      if (getCode(secretBytes, timeIndex + i) == code) { return true; }
    }
    false
  }

  def getCode(secret: Array[Byte], timeIndex: Long): Int = {
    val msg = ByteBuffer.allocate(8).putLong(timeIndex).array()
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret, "RAW"))
    val hash = mac.doFinal(msg)
    val offset = hash(hash.length - 1) & 0xf
    val binary = ((hash(offset) & 0x7f) << 24) | ((hash(offset + 1) & 0xff) << 16) | ((hash(offset + 2) & 0xff) << 8) | (hash(offset + 3) & 0xff)
    (binary % 1000000).toInt
  }
}