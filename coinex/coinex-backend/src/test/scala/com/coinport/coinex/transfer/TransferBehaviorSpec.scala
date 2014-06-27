/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.transfer

import com.coinport.coinex.data._
import com.coinport.coinex.common.EmbeddedMongoForTestWithBF
import akka.event.LoggingAdapter

class TransferBehaviorSpec extends EmbeddedMongoForTestWithBF {
  "AccountTransferWithdrawSpec" should {
    val dw = new AccountTransferBehavior {
      implicit val logger: LoggingAdapter = null //Cann't log anything
      val db = database
      val manager = new AccountTransferManager
    }
    "be able to save transferHandler and query them" in {
      val d1 = AccountTransfer(id = 1, userId = 1, `type` = TransferType.Deposit, currency = Currency.Cny, amount = 1000, created = Some(100), updated = Some(800))
      val d2 = AccountTransfer(id = 2, userId = 1, `type` = TransferType.Deposit, currency = Currency.Btc, amount = 2000, created = Some(200), updated = Some(800))
      val d3 = AccountTransfer(id = 3, userId = 2, `type` = TransferType.Deposit, currency = Currency.Cny, amount = 1000, created = Some(300), updated = Some(800))
      val d4 = AccountTransfer(id = 4, userId = 2, `type` = TransferType.Deposit, currency = Currency.Btc, amount = 2000, created = Some(400), updated = Some(800))
      val seq = Seq(d1, d2, d3, d4)
      seq.foreach(d => dw.transferHandler.put(d))

      var q = QueryTransfer(cur = Cursor(0, 10))
      dw.transferHandler.count(dw.transferHandler.getQueryDBObject(q)) should be(4)

      q = QueryTransfer(uid = Some(1), cur = Cursor(0, 10))
      dw.transferHandler.count(dw.transferHandler.getQueryDBObject(q)) should be(2)

      q = QueryTransfer(uid = Some(1), cur = Cursor(0, 10))
      dw.transferHandler.find(dw.transferHandler.getQueryDBObject(q), 0, 10).map(_.id) should equal(Seq(2, 1))

      q = QueryTransfer(uid = Some(1), currency = Some(Currency.Cny), cur = Cursor(0, 10))
      dw.transferHandler.count(dw.transferHandler.getQueryDBObject(q)) should be(1)

      q = QueryTransfer(uid = Some(1), currency = Some(Currency.Cny), cur = Cursor(0, 10))
      dw.transferHandler.find(dw.transferHandler.getQueryDBObject(q), 0, 10).map(_.id) should equal(Seq(1))

      q = QueryTransfer(spanCur = Some(SpanCursor(300, 200)), cur = Cursor(0, 10))
      dw.transferHandler.count(dw.transferHandler.getQueryDBObject(q)) should be(2)

      q = QueryTransfer(spanCur = Some(SpanCursor(300, 200)), cur = Cursor(0, 10))
      dw.transferHandler.find(dw.transferHandler.getQueryDBObject(q), 0, 10).map(_.id) should equal(Seq(3, 2))
    }
  }
}
