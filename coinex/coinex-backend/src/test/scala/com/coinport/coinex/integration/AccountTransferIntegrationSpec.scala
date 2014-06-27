package com.coinport.coinex.integration

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import scala.concurrent.duration._
import akka.actor.actorRef2Scala

// TODO(d): somehow the deposits are not saved into the right mongodb collection.
// TODO(d): complete the spec.
class AccountTransferIntegrationSpec extends IntegrationSpec(new Environment) {
  import env._
  "CoinexApp" must {
    "save and retrieve deposit requests" in {
      val deposit = AccountTransfer(1, 10000, TransferType.Deposit, Cny, 500000000L, TransferStatus.Pending)
      client ! DoRequestTransfer(deposit)
      val RequestTransferSucceeded(d) = receiveOne(4 seconds)
      d.status should be(TransferStatus.Pending)
      d.created shouldNot be(None)
      d.id should be(1E12.toLong + 1)
      Thread.sleep(1000)
    }
  }
}