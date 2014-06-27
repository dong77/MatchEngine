package com.coinport.coinex.robot.exchange

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random
import scala.util.parsing.json.JSON.parseFull
import com.coinport.coinex.Coinex
import com.coinport.coinex.LocalRouters
import com.coinport.coinex.api.model.UserOrder
import com.coinport.coinex.api.model.Operations
import com.coinport.coinex.api.service.AccountService
import com.coinport.coinex.data.Currency.Btc
import com.coinport.coinex.data.Currency.Doge
import com.coinport.coinex.data.Currency.Ltc
import com.coinport.coinex.data.Implicits.currency2Rich
import com.coinport.coinex.data.MarketSide
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.cluster.Cluster
import akka.util.Timeout
import dispatch.Http
import dispatch.as
import dispatch.enrichFuture
import dispatch.implyRequestHandlerTuple
import dispatch.url
import com.coinport.coinex.api.model.ApiUserAccount
import scala.concurrent.Future

class AutoExchangeRobotExecutor(marketUrlMap: Map[MarketSide, String], marketUpdateInterval: Long) {

  case class User(email: String, exchangeFrequency: Int = 5, takerPercentage: Int = 90, riskPercentage: Int = 10, buyPercentage: Int = 50)
  case class DepthElem(price: Double, quantity: Double)

  var depthBuy: Map[MarketSide, List[DepthElem]] = Map.empty[MarketSide, List[DepthElem]]
  var depthSell: Map[MarketSide, List[DepthElem]] = Map.empty[MarketSide, List[DepthElem]]

  var userMap: Map[Long, User] = Map.empty[Long, User]

  def updateDepth() {
    marketUrlMap.map { kv =>
      val (sell, buy) = jsonToDepth(getDepthJsonByUrl(kv._2))
      depthSell += kv._1 -> sell
      depthBuy += kv._1 -> buy
    }
  }

  def jsonToDepth(json: String): (List[DepthElem], List[DepthElem]) = {
    val jsonData = parseFull(json)
    val depthBuyList = transfer(jsonData.get.asInstanceOf[Map[String, Any]].get("bids").get.asInstanceOf[List[List[Any]]])
    val depthSellList = transfer(jsonData.get.asInstanceOf[Map[String, Any]].get("asks").get.asInstanceOf[List[List[Any]]])
    (depthSellList, depthBuyList.reverse)
  }

  def startExecutor() {

    // start update depth
    new Thread(new Runnable {
      def run() {
        while (true) {
          updateDepth()
          Thread.sleep(marketUpdateInterval)
        }
      }
    }).start()

    Thread.sleep(marketUpdateInterval * 2)

    // add default users
    initDefaultUser

    userMap.map { kv =>

      new Thread(new Runnable {
        def run() {
          while (true) {
            println("start submit order User : " + kv._2)
            createOrder(kv._1, kv._2) foreach {
              o => submitOrder(o)
            }
            val interval = kv._2.exchangeFrequency * 1000 / 2 + Random.nextInt(kv._2.exchangeFrequency * 1000)
            println("interval " + interval)
            Thread.sleep(interval)
          }
        }
      }).start()
    }
  }

  def initDefaultUser() {
    userMap ++= Map(
      1000000001L -> User("test1@coinport.com"),
      1000000000L -> User("test2@coinport.com"))
    //          1000000000L -> User("c@coinport.com"),
    //          1000000001L -> User("weichao@coinport.com"),
    //          1000000004L -> User("chunming02@163.com"),
    //          1000000008L -> User("jaice_229@163.com"),
    //          1000000003L -> User("yangli@coinport.com"),
    //          1000000016L -> User("kongliang@coinport.com"),
    //          1000000015L -> User("chenxi@coinport.com"))
  }

  def transfer(list: List[List[Any]]): List[DepthElem] = {
    var depthList: List[DepthElem] = List()
    list.map { f =>
      val a = f(0) match {
        case i: String => i.toDouble
        case i: Double => i
        case _ => 0.0
      }
      val b = f(1) match {
        case i: String => i.toDouble
        case i: Double => i
        case _ => 0.0
      }
      depthList = DepthElem(a, b) :: depthList
    }
    depthList
  }

  def getDepthJsonByUrl(targetUrl: String): String = {
    val result = Http(url(targetUrl) OK as.String)
    result()
  }

  def getAccount(uid: Long, currency: String): Future[Double] = {
    var amount = 0.0
    val result = AccountService.getAccount(uid)
    result map {
      case m =>
        println("get account result : " + m)
        val account = m.data.get.asInstanceOf[ApiUserAccount]
        println("account" + account)
        val aa = account.accounts(currency).available.value
        println("aa>>>>>>>>>>> " + aa)
        aa
    }
  }

  def createOrder(uid: Long, user: User): Future[UserOrder] = {
    val sides = marketUrlMap.keySet.toArray
    val side = sides(Random.nextInt(sides.size))
    val isTaker = hit(user.takerPercentage)

    val priceTerm = 5
    var price = 0.0
    var quantity = 0.0
    var operationsType = Operations.Buy

    (hit(user.takerPercentage), hit(user.buyPercentage)) match {
      case (true, true) => {
        operationsType = Operations.Buy
        price = depthSell(side).drop(Random.nextInt(priceTerm)).head.price
        getAccount(uid, side._1.name.toUpperCase) map {
          q =>
            quantity = q * user.riskPercentage / (100 * price)
            UserOrder(uid.toString, operationsType, side._2.name, side._1.name, Some(price), Some(quantity), None)
        }
      }
      case (true, false) => {
        operationsType = Operations.Sell
        price = depthBuy(side).drop(Random.nextInt(priceTerm)).head.price
        getAccount(uid, side._2.name.toUpperCase) map {
          q =>
            quantity = q * user.riskPercentage / 100
            UserOrder(uid.toString, operationsType, side._2.name, side._1.name, Some(price), Some(quantity), None)
        }
      }
      case (false, true) => {
        operationsType = Operations.Buy
        val high = depthBuy(side).head.price
        val low = depthBuy(side).last.price
        price = high - (high - low) * gaussRandom(high, low)
        if (price < 0.0) {
          price = low
        }
        getAccount(uid, side._1.name.toUpperCase) map {
          q =>
            quantity = q * user.riskPercentage / (100 * price)
            UserOrder(uid.toString, operationsType, side._2.name, side._1.name, Some(price), Some(quantity), None)
        }
      }
      case (false, false) => {
        operationsType = Operations.Sell
        val high = depthSell(side).last.price
        val low = depthSell(side).head.price
        price = low + (high - low) * gaussRandom(high, low)
        getAccount(uid, side._2.name.toUpperCase) map {
          q =>
            quantity = q * user.riskPercentage / 100
            UserOrder(uid.toString, operationsType, side._2.name, side._1.name, Some(price), Some(quantity), None)
        }
      }
    }

  }

  def submitOrder(order: UserOrder) {
    println("submit order : " + order)
    AccountService.submitOrder(order)
  }

  def hit(percent: Int): Boolean = {
    if (Random.nextInt(100) > percent)
      false
    else
      true
  }

  def gaussRandom(max: Double, min: Double): Double = {
    val temp = 12.0
    var x = 0.0
    0 to temp.toInt foreach { i =>
      x = x + Random.nextDouble
    }
    x = (x - temp / 2) / (Math.sqrt(temp * 1.5))
    Math.abs(x)
  }

}

object AutoExchangeRobotExecutor {

  def main(args: Array[String]) {
    val marketUrlMap: Map[MarketSide, String] = Map(Btc ~> Ltc -> "http://data.bter.com/api/1/depth/ltc_btc", Btc ~> Doge -> "http://data.bter.com/api/1/depth/doge_btc")
    val executor = new AutoExchangeRobotExecutor(marketUrlMap, 5000)
    executor.startExecutor

    //    var t: Map[Double, Int] = Map.empty
    //    val high = 17.0
    //    val low = 4.0
    //    1 to 10000 foreach {_ =>
    //      
    //      val k = (high - (high - low) * executor.gaussRandom(high, low)).ceil
    //      if (t.contains(k)) {
    //        t += k -> (t(k) + 1)
    //      } else {
    //        t += k -> 0
    //      }
    //    
    //    }
    //    println(t)

  }

  def start() {
    val marketUrlMap: Map[MarketSide, String] = Map(Btc ~> Ltc -> "http://data.bter.com/api/1/depth/ltc_btc", Btc ~> Doge -> "http://data.bter.com/api/1/depth/doge_btc")
    val executor = new AutoExchangeRobotExecutor(marketUrlMap, 5000)
    executor.startExecutor
  }

}
