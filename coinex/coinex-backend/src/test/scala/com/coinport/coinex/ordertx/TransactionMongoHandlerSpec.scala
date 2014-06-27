/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.ordertx

import com.coinport.coinex.data.Currency.{ Cny, Btc }
import com.coinport.coinex.data._
import com.coinport.coinex.common.EmbeddedMongoForTestWithBF
import com.coinport.coinex.data.Implicits._

class TransactionMongoHandlerSpec extends EmbeddedMongoForTestWithBF {
  val market = Btc ~> Cny

  class TransactionClass extends TransactionBehavior {
    val coll = database("transaction")
  }

  "TransactionHandlerSpec" should {
    val transactionClass = new TransactionClass()

    "add item into state and get them all" in {
      transactionClass.coll.drop()
      val txs = (0 until 10) map (i => Transaction(i, i, market, OrderUpdate(Order(i, i, i), Order(i, i, i)), OrderUpdate(Order(i, i, i), Order(i, i, i))))
      txs.foreach(t => transactionClass.addItem(t))

      var q = QueryTransaction(cursor = Cursor(0, 2))
      transactionClass.getItems(q).map(_.id) should equal(Seq(9, 8))

      q = QueryTransaction(cursor = Cursor(0, 1))
      transactionClass.countItems(q) should be(10)

      q = QueryTransaction(cursor = Cursor(0, 100))
      transactionClass.getItems(q).map(_.id) should equal(Seq(9, 8, 7, 6, 5, 4, 3, 2, 1, 0))
    }
  }

}
