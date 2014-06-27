package com.coinport.coinex.integration

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import scala.concurrent.duration._
import akka.actor.actorRef2Scala
import org.junit.Before
import Implicits._

object MarketIntegrationSpec {
  var id = 0L
  def getAccountTransferId: Long = {
    id += 1
    id
  }
}

class MarketIntegrationSpec extends IntegrationSpec(new Environment) {
  import env._

  val market = MarketSide(Btc, Cny)
  val reverse = MarketSide(Cny, Btc)
  val user1 = 1000L
  val user2 = 2000L
  val user3 = 3000L
  val user4 = 4000L
  val user5 = 5000L
  // deposit 6000 CNY
  deposit(user1, Cny, 6000 * 1000)
  deposit(user3, Cny, 6000 * 1000)
  deposit(user4, Cny, 6000 * 1000)

  // deposit 10 BTC
  deposit(user2, Btc, 10 * 1000)
  deposit(user3, Btc, 10 * 1000)
  deposit(user5, Btc, 10 * 1000)

  "CoinexApp" must {

    "deposit should be correct" in {
      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(user1, Map(Cny -> CashAccount(Cny, 6000000, 0, 0)))))

      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(user2, Map(Btc -> CashAccount(Btc, 10000, 0, 0)))))

      client ! QueryAccount(user3)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(user3, Map(Cny -> CashAccount(Cny, 6000000, 0, 0), Btc -> CashAccount(Btc, 10000, 0, 0)))))

    }

    "confirm correct exchange when takeLimit < quantity * price(include refund condition of hit limit)" in {

      // submit a sell order(takeLimit < quantity * price)
      val sellBtc = Order(userId = user2, id = 0L, quantity = 10000, price = Some(500.0), takeLimit = Some(4500000))
      client ! DoSubmitOrder(market, sellBtc)

      val resultSellBtc = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Cny -> CashAccount(Cny, 6000000, 0, 0)))))

      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 1000, 9000, 0)))))

      // submit a buy order
      val buyBtc = Order(userId = user1, id = 0L, quantity = 6000000, price = Some(600 reciprocal), takeLimit = Some(10000))
      client ! DoSubmitOrder(reverse, buyBtc)

      val resultBuyBtc = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Cny -> CashAccount(Cny, 900000, 600000, 0), Btc -> CashAccount(Btc, 8991, 0, 0)))))

      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 1000, 0, 0), Cny -> CashAccount(Cny, 4495500, 0, 0)))))
    }

    "confirm correct exchange when takeLimit > quantity * price" in {
      // submit a sell order(takeLimit > quantity * price)
      val sellBtc2 = Order(userId = user1, id = 0L, quantity = 2000, price = Some(700.0), takeLimit = Some(1500000))
      client ! DoSubmitOrder(market, sellBtc2)

      val resultSellBtc2 = receiveOne(4 seconds)
      Thread.sleep(500)

      // submit a buy order(takeLimit < quantity * price)
      val buyBtc2 = Order(userId = user2, id = 0L, quantity = 2000000, price = Some(700 reciprocal), takeLimit = Some(10000))
      client ! DoSubmitOrder(reverse, buyBtc2)

      val resultBuyBtc2 = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Cny -> CashAccount(Cny, 2298600, 600000, 0), Btc -> CashAccount(Btc, 6991, 0, 0)))))

      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 2998, 0, 0), Cny -> CashAccount(Cny, 2495500, 600000, 0)))))
    }

    "confirm correct exchange when taker takes multiple pending orders" in {
      val buyBtc3_1 = Order(userId = user3, id = 0L, quantity = 2000000, price = Some(700 reciprocal), takeLimit = Some(2000))
      client ! DoSubmitOrder(reverse, buyBtc3_1)
      val resultBuyBtc3_1 = receiveOne(4 seconds)
      Thread.sleep(500)
      val buyBtc3_2 = Order(userId = user3, id = 0L, quantity = 2000000, price = Some(800 reciprocal), takeLimit = Some(2500))
      client ! DoSubmitOrder(reverse, buyBtc3_2)
      val resultBuyBtc3_2 = receiveOne(4 seconds)
      Thread.sleep(500)
      val buyBtc3_3 = Order(userId = user3, id = 0L, quantity = 2000000, price = Some(1000 reciprocal), takeLimit = Some(3000))
      client ! DoSubmitOrder(reverse, buyBtc3_3)
      val resultBuyBtc3_3 = receiveOne(4 seconds)
      Thread.sleep(500)
      val sellBtc3_1 = Order(userId = user1, id = 0L, quantity = 6000, price = Some(800.0), takeLimit = Some(4800000))
      client ! DoSubmitOrder(market, sellBtc3_1)
      val resultSellBtc3_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Cny -> CashAccount(Cny, 6294600, 600000, 0), Btc -> CashAccount(Btc, 1491, 1000, 0)))))

      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 2998, 0, 0), Cny -> CashAccount(Cny, 2495500, 600000, 0)))))

      client ! QueryAccount(user3)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(3000, Map(Cny -> CashAccount(Cny, 600000, 1400000, 0), Btc -> CashAccount(Btc, 14495, 0, 0)))))
    }

    "confirm exchange and refund is right when onlyTaker is true(include refund condition of auto cancelled)" in {
      val buyBtc4_1 = Order(userId = user4, id = 0L, quantity = 4000000, price = Some(1000 reciprocal), takeLimit = Some(2000))
      client ! DoSubmitOrder(reverse, buyBtc4_1)
      val resultBuyBtc4_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      val sellBtc4_1 = Order(userId = user5, id = 0L, quantity = 6000, price = Some(1000.0), takeLimit = Some(6000000), onlyTaker = Some(true))
      client ! DoSubmitOrder(market, sellBtc4_1)
      val resultSellBtc4_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Cny -> CashAccount(Cny, 7093800, 600000, 0), Btc -> CashAccount(Btc, 1491, 0, 0)))))
      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 2998, 0, 0), Cny -> CashAccount(Cny, 2495500, 600000, 0)))))
      client ! QueryAccount(user3)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(3000, Map(Cny -> CashAccount(Cny, 600000, 1400000, 0), Btc -> CashAccount(Btc, 14495, 0, 0)))))
      client ! QueryAccount(user4)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(4000, Map(Cny -> CashAccount(Cny, 4200000, 0, 0), Btc -> CashAccount(Btc, 1998, 0, 0)))))
      client ! QueryAccount(user5)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(5000, Map(Btc -> CashAccount(Btc, 9000, 0, 0), Cny -> CashAccount(Cny, 999000, 0, 0)))))
    }

    "confirm the dust is refunded to user(include refund condition of dust)" in {
      val buyBtc5_1 = Order(userId = user4, id = 0L, quantity = 2000000, price = Some(700 reciprocal), takeLimit = Some(10000))
      client ! DoSubmitOrder(reverse, buyBtc5_1)
      val resultBuyBtc5_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      val sellBtc5_1 = Order(userId = user5, id = 0L, quantity = 6000, price = Some(700.0), takeLimit = Some(4200000))
      client ! DoSubmitOrder(market, sellBtc5_1)
      val resultSellBtc5_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Btc -> CashAccount(Btc, 1491, 0, 0), Cny -> CashAccount(Cny, 7093800, 600000, 0)))))
      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 3854, 0, 0), Cny -> CashAccount(Cny, 2495600, 0, 0)))))
      client ! QueryAccount(user3)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(3000, Map(Btc -> CashAccount(Btc, 16493, 0, 0), Cny -> CashAccount(Cny, 600000, 0, 0)))))
      client ! QueryAccount(user4)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(4000, Map(Btc -> CashAccount(Btc, 4852, 0, 0), Cny -> CashAccount(Cny, 2200100, 0, 0)))))
      client ! QueryAccount(user5)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(5000, Map(Btc -> CashAccount(Btc, 3000, 286, 0), Cny -> CashAccount(Cny, 4994800, 0, 0)))))
    }

    "confirm the over charged amount is refunded to user(include refund condition of over charge)" in {
      val buyBtc6_1 = Order(userId = user1, id = 0L, quantity = 4000000, price = Some(2000 reciprocal), takeLimit = Some(2000))
      client ! DoSubmitOrder(reverse, buyBtc6_1)
      val resultBuyBtc6_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      val sellBtc6_1 = Order(userId = user3, id = 0L, quantity = 2000, price = Some(1900.0), takeLimit = Some(3800000))
      client ! DoSubmitOrder(market, sellBtc6_1)
      val resultSellBtc6_1 = receiveOne(4 seconds)
      Thread.sleep(500)

      client ! QueryAccount(user1)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(1000, Map(Btc -> CashAccount(Btc, 3489, 0, 0), Cny -> CashAccount(Cny, 3465600, 600000, 0)))))
      client ! QueryAccount(user2)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(2000, Map(Btc -> CashAccount(Btc, 3854, 0, 0), Cny -> CashAccount(Cny, 2495600, 0, 0)))))
      client ! QueryAccount(user3)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(3000, Map(Btc -> CashAccount(Btc, 14583, 196, 0), Cny -> CashAccount(Cny, 4024572, 0, 0)))))
      client ! QueryAccount(user4)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(4000, Map(Btc -> CashAccount(Btc, 4852, 0, 0), Cny -> CashAccount(Cny, 2200100, 0, 0)))))
      client ! QueryAccount(user5)
      receiveOne(4 seconds) should be(QueryAccountResult(UserAccount(5000, Map(Btc -> CashAccount(Btc, 3000, 0, 0), Cny -> CashAccount(Cny, 5194800, 0, 0)))))
    }

  }

  private def deposit(userId: Long, currency: Currency, amount: Long) {
    val deposit = AccountTransfer(MarketIntegrationSpec.getAccountTransferId, userId, TransferType.Deposit, currency, amount, TransferStatus.Pending)
    client ! DoRequestTransfer(deposit)
    val RequestTransferSucceeded(d) = receiveOne(4 seconds)

    client ! AdminConfirmTransferSuccess(d)
    val ok = receiveOne(4 seconds)
    ok should be(AdminCommandResult(ErrorCode.Ok))
    Thread.sleep(200)
  }

}
