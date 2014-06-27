package com.coinport.coinex.common.mongo

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.common._

class SimpleMongoCollectionSpec extends EmbeddedMongoForTestWithBF {

  val jsonTransfers = new SimpleJsonMongoCollection[AccountTransfer, AccountTransfer.Immutable]() {
    val coll = database("transfer_json")
    def extractId(t: AccountTransfer) = t.id
  }

  "SimpleJsonMongoCollection" must {
    "save and retrieve account transfers" in {
      val deposit = AccountTransfer(1, 2, TransferType.Deposit, Cny, 123, TransferStatus.Pending)
      jsonTransfers.put(deposit)
      jsonTransfers.get(1) should be(Some(deposit))
      jsonTransfers.get(2) should be(None)
    }
  }

  val binaryTransfers = new SimpleBinaryMongoCollection[AccountTransfer, AccountTransfer.Immutable]() {
    val coll = database("transfer_binary")
    def extractId(t: AccountTransfer) = t.id
  }

  "SimpleBinaryMongoCollection" must {
    "save and retrieve deposits" in {
      val deposit = AccountTransfer(1, 2, TransferType.Deposit, Cny, 123, TransferStatus.Pending)
      binaryTransfers.put(deposit)
      binaryTransfers.get(1) should be(Some(deposit))
      binaryTransfers.get(2) should be(None)
    }
  }
}
