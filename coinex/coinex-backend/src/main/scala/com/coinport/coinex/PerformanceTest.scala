package com.coinport.coinex.performance

import scala.util.Random
import com.coinport.coinex.Client
import com.coinport.coinex.api.service.AccountService
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import akka.actor._
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import Implicits._

object PerformanceTest {

  implicit val timeout = Timeout(1000 seconds)
  val btcSide = MarketSide(Btc, Cny)
  val rmbSide = MarketSide(Cny, Btc)

  def before {
    // register users
    Client.registerTestUsers
    // deposit
    Client.userMap foreach { kv =>
      AccountService.deposit(kv._2, Btc, 1000000 * 1000)
      AccountService.deposit(kv._2, Cny, 1000000000 * 100)
    }
    Thread.sleep(4000)
  }

  def test(cycle: Int, warmup: Long, endCycle: Long) {
    ////////// register user and deposit
    before

    var count = 0L
    var errorCount = 0L

    var startTime: Long = 0L
    var endTime: Long = 0L

    ////////// prepare test data
    var dataList = List.empty[(Long, MarketSide, Double, Double)]
    1 to cycle foreach { _ =>
      val side = List(btcSide, rmbSide)(Random.nextInt(2))
      var quantity = 0d
      var orderPrice = 0d
      if (side == btcSide) {
        quantity = Random.nextDouble() + Random.nextInt(1000)
        orderPrice = 250 + Random.nextInt(100)
      } else {
        quantity = 100000 + Random.nextInt(900000)
        orderPrice = 1.0 / (250 + Random.nextInt(100))
      }
      val userId = Client.userMap.values.toList(Random.nextInt(Client.userMap.size))
      dataList = dataList.::((userId, side, quantity, orderPrice))
    }

    ////////// execute test
    1 to cycle foreach { i =>
      val j = i - 1
      val f = Client.backend ? DoSubmitOrder(
        dataList(j)._2,
        Order(dataList(j)._1, 0, dataList(j)._3.toLong, price = Some(dataList(j)._4), robotId = Some(dataList(j)._1)))

      f onSuccess {
        case m => {
          count += 1
          if (count == warmup) {
            startTime = System.currentTimeMillis
            println("=" * 50)
            println("startTime : " + startTime)
          } else if (count == endCycle) {
            endTime = System.currentTimeMillis
            println("endTime : " + endTime)
            println("execute Time : " + (endTime - startTime))
            println("qps : " + (endCycle - warmup) * 1000.0 / (endTime - startTime))
            println("=" * 50)
          }
        }
      }

      f onFailure {
        case m => {
          errorCount += 1
          println("execute total error count: " + errorCount)
          println("response error: " + m)
          return
        }
      }
    }
  }

  def continusPressureTest(qps: Int, cycle: Int) {
    ////////// register user and deposit
    before

    val startTime = System.currentTimeMillis
    var errorCount = 0L
    val btcSide = MarketSide(Btc, Cny)
    val rmbSide = MarketSide(Cny, Btc)

    ////////// prepare test data
    var dataList = List.empty[(Long, MarketSide, Double, Double)]
    1 to cycle foreach { _ =>
      val side = List(btcSide, rmbSide)(Random.nextInt(2))
      var quantity = 0d
      var orderPrice = 0d
      if (side == btcSide) {
        quantity = Random.nextDouble() + Random.nextInt(1000)
        orderPrice = 250 + Random.nextInt(100)
      } else {
        quantity = 100000 + Random.nextInt(900000)
        orderPrice = 1.0 / (250 + Random.nextInt(100))
      }
      val userId = Client.userMap.values.toList(Random.nextInt(Client.userMap.size))
      dataList = dataList.::((userId, side, quantity, orderPrice))
    }

    ////////// execute test
    1 to cycle foreach { i =>
      val sleepTime = Random.nextInt((1000 / qps) * 2)

      if (i == (cycle - 100)) {
        val endTime = System.currentTimeMillis
        println("execute time " + (endTime - startTime))
      }
      if (i % 10 == 0) Thread.sleep(sleepTime * 10)

      if (i % 1000 == 0) println("sent >>>>> " + i)
      val j = i - 1
      val f = Client.backend ? DoSubmitOrder(
        dataList(j)._2,
        Order(dataList(j)._1, 0, dataList(j)._3.toLong, price = Some(dataList(j)._4), robotId = Some(dataList(j)._1)))

      f onFailure {
        case m => {
          errorCount += 1
          println("execute total error count: " + errorCount)
          println("response error: " + m)
          return
        }
      }
    }
  }

  def marketDepthViewQpsTest(cycle: Int) = viewQpsTest(cycle, QueryMarketDepth(btcSide, 100))

  def candleDataViewQpsTest(cycle: Int) = {
    val from = System.currentTimeMillis - 10000000L
    val to = from + 4000000L
    viewQpsTest(cycle, QueryCandleData(btcSide, ChartTimeDimension.get(Random.nextInt(12) + 1).get, from, to))
  }

  def metricsViewQpsTest(cycle: Int) = viewQpsTest(cycle, QueryMetrics)

  def viewQpsTest(cycle: Int, msg: Any) {
    val startTime = System.currentTimeMillis
    println("=" * 50)
    println("startTime : " + startTime)
    1 to cycle foreach { i =>
      val f = Client.backend ? msg
      f onSuccess {
        case m => {

          if (i % 1000 == 0) println(m)

          if (i == (cycle - 1)) {
            val endTime = System.currentTimeMillis
            println("endTime : " + endTime)
            println("qps = " + (cycle * 1000 / (endTime - startTime)))
            println("=" * 50)
          }
        }
      }
      f onFailure { case m => println(m) }
    }
  }

  def main(args: Array[String]) {

    args(0) match {
      case "test" => test(args(1).toInt, args(2).toInt, args(3).toInt)
      case "continusPressureTest" => continusPressureTest(args(1).toInt, args(2).toInt)
      case "marketDepthViewQpsTest" => marketDepthViewQpsTest(args(1).toInt)
      case _ => println("usage: \nClient add 100    -- add 100 robots\nrm    -- remove all robots")
    }
  }
}
