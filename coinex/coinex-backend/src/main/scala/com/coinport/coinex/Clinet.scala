/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex

import scala.concurrent.duration._
import com.coinport.coinex.api.service._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.EmailType._
import com.coinport.coinex.data.Implicits._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.pattern.ask
import akka.actor._
import akka.util.Timeout
import com.coinport.coinex.robot.sample.StopOrderRobot
import scala.concurrent.ExecutionContext.Implicits.global

object Client {
  implicit val timeout = Timeout(10 seconds)

  val configPath = System.getProperty("akka.config") match {
    case null => "akka.conf"
    case c => c
  }
  private val config = ConfigFactory.load(configPath)
  private implicit val system = ActorSystem("coinex", config)
  private implicit val cluster = Cluster(system)
  private val markets = Seq(Ltc ~> Btc, Doge ~> Btc)
  val routers = new LocalRouters(markets)

  val backend = system.actorOf(Props(new Coinex(routers)), name = "backend")
  println("Example: Client.backend ! SomeMessage()")

  val userMap = Map(
    "wd" -> -245561917658914311L,
    "mc" -> -6771488127296557565L,
    "lcm" -> 877800447188483646L,
    "cx" -> 91990591289398244L,
    "lwc" -> 5742988204510593740L,
    "xl" -> 11190591289398244L,
    "kl" -> 22290591289398244L)

  var robotMap: Map[String, Long] = Map.empty[String, Long]

  var basicRisk = 10.0
  val risk = userMap.map(kv => { basicRisk += 10.0; (kv._2 -> basicRisk) })

  var did = -1

  def registerDepositUsers = {
    // register default user and deposit
    userMap.map(kv => registerUser(kv._2, kv._1 + "@coinport.com", kv._1))
    userMap foreach { kv =>
      AccountService.deposit(kv._2, Btc, 10000 * 1000)
      AccountService.deposit(kv._2, Ltc, 1000000 * 100)
    }
  }

  def addGofvRobots(num: Int) {

    1 to num foreach { i =>
      robotMap += i.toString -> (10000 + i.toLong)
    }

    robotMap foreach { kv =>
      val uid = kv._2
      registerUser(uid, uid + "@coinport.com", uid.toString)
    }
    Thread.sleep(2000)
    robotMap foreach { kv =>
      val uid = kv._2
      AccountService.deposit(uid, Btc, 10000 * 1000)
      AccountService.deposit(uid, Ltc, 1000000 * 100)
    }
    Thread.sleep(4000)

    robotMap foreach { kv =>

      val uid = kv._2
      val dna = Map(
        "START" -> """
        (robot -> "LOOP", None)
        """,

        "LOOP" -> """
        import scala.util.Random
        import com.coinport.coinex.data._
        import com.coinport.coinex.data.Currency._

        var counter = robot.getPayload[Int]("COUNTER").get
        println(counter)
        val r = robot.setPayload("COUNTER", Some(counter+1))
        val btcSide = MarketSide(Btc, Ltc)
        val rmbSide = MarketSide(Ltc, Btc)
        val side = List(btcSide, rmbSide)(Random.nextInt(2))
        val price = metrics match {
          case None => if (side == btcSide) 3000.0 else 1 / 3000.0
          case Some(m) => m.metricsByMarket.get(side) match {
            case Some(mbm) => mbm.price
            case _ => if (side == btcSide) 3000.0 else 1 / 3000.0
          }
        }

        val range = %f - Random.nextInt(100)
        var orderPrice = price * (1 + range / 100.0)
        var quantity = 10 * (1 + range / 100.0)
        if (side == rmbSide) quantity /= orderPrice
        if (side == btcSide) {
          quantity = Random.nextDouble() + Random.nextInt(1000)
          orderPrice = 250 + Random.nextInt(100)
        } else {
          quantity = 100000 + Random.nextInt(900000)
          orderPrice = 1.0/(250 + Random.nextInt(100))
        }
        val action = Some(DoSubmitOrder(side,
    Order(robot.userId, 0, quantity.toLong, price = Some(RDouble(orderPrice, false)), robotId = Some(robot.robotId))))
        (r -> "LOOP", action)
        """.format(50.0))

      val order = Order(1L, 2L, 10L, inAmount = 30L)
      val payload: Map[String, Option[Any]] =
        Map("SP" -> Some(120L), "ORDER" -> Some(order), "COUNTER" -> Some(101), "SIDE" -> Some("tttt"))
      Client.backend ? DoAddRobotDNA(dna) map {
        case AddRobotDNAFailed(ErrorCode.RobotDnaExist, existingDNAId) =>
          val payload = Map("SP" -> Some(120L), "ORDER" -> Some(order), "COUNTER" -> Some(101), "SIDE" -> Some("tttt"))
          val robot = Robot(uid, uid, uid, dnaId = existingDNAId, payloads = payload)
          println("exist robot dna >>>> id: " + existingDNAId)
          Client.backend ! DoSubmitRobot(robot)
        case mid =>
          val robot = Robot(uid, uid, uid, dnaId = mid.asInstanceOf[Long], payloads = payload)
          println("generate robot >>>> id: " + robot.robotId)
          Client.backend ! DoSubmitRobot(robot)
      }
    }
  }

  def removeGofvRobots {
    userMap foreach { kv =>
      Client.backend ! DoCancelRobot(kv._2)
    }
    robotMap foreach { kv =>
      val uid = kv._2
      Client.backend ! DoCancelRobot(uid)
    }

  }

  def registerUser(uid: Long, mail: String, pwd: String) {
    Client.backend ! DoRegisterUser(
      UserProfile(
        id = uid,
        email = mail,
        emailVerified = false,
        mobileVerified = false,
        status = UserStatus.Normal),
      pwd)
    println("add user >>>> " + mail)
  }

  def deposit(uid: Long, amount: Double) =
    AccountService.deposit(uid, Currency.Ltc, amount)

  def createABCode(wUserId: Long, amount: Long, dUserId: Long) {
    Client.backend ? DoRequestGenerateABCode(wUserId, amount, None, None) map {
      case RequestGenerateABCodeFailed(ErrorCode.InsufficientFund) => println("create ab code failed")
      case RequestGenerateABCodeSucceeded(a, b) => {
        println("a code: " + a + " b code: " + b)
        Client.backend ? DoRequestACodeQuery(dUserId, a) map {
          case RequestACodeQuerySucceeded(x, y, z) => println(x, y, z)
        }
      }
      case el => println(el)
    }
  }

  def verifyAcode(userId: Long, codeA: String) {
    Client.backend ? DoRequestACodeQuery(userId, codeA) map {
      case RequestACodeQuerySucceeded(x, y, z) => println(x, y, z)
      case RequestACodeQueryFailed(ErrorCode.LockedACode) => println("locked")
    }
  }

  def recharge(userId: Long, codeB: String) {
    Client.backend ? DoRequestBCodeRecharge(userId, codeB) map {
      case m: RequestBCodeRechargeFailed => println(m)
      case RequestBCodeRechargeSucceeded(x, y, z) => println(x, y, z)
    }
  }

  def comfirm(userId: Long, codeB: String, amount: Long) {
    Client.backend ? DoRequestConfirmRC(userId, codeB, amount) map {
      case RequestConfirmRCSucceeded(x, y, z) => println(x, y, z)
      case m => println(m)
    }
  }

  def queryRCDepositRecord(userId: Long) {
    Client.backend ? QueryRCDepositRecord(userId) map {
      case m => println(m)
    }
  }

  def queryRCWithdrawalRecord(userId: Long) {
    Client.backend ? QueryRCWithdrawalRecord(userId) map {
      case m => println(m)
    }
  }

  def registerTestUsers {
    userMap.map(kv => registerUser(kv._2, kv._1 + "@coinport.com", kv._1))
  }

  def getBtcAddress = {
    Client.backend ? AllocateNewAddress(Btc, 1425L) map { i =>
      println(i)
    }
  }

  def faucetBtcUserAddress = {
    Client.backend ! BitwayMessage(Btc, generateAddressResponse = Some(GenerateAddressesResult(ErrorCode.Ok, Some(Set(CryptoAddress("userAddress1"))), Some(CryptoCurrencyAddressType.Unused))))
  }

  // userAddress1
  def sendTxMessage(address: String, sigId: String = "mockSigId1") = {
    Client.backend ! BitwayMessage(Btc, tx = Some(CryptoCurrencyTransaction(
      sigId = Some(sigId),
      ids = Some(List(1425L)),
      inputs = Some(List(CryptoCurrencyTransactionPort("inputAddr1", Some(1.0)))),
      outputs = Some(List(CryptoCurrencyTransactionPort(address, Some(1.0), userId = Some(1425L)))),
      prevBlock = Some(BlockIndex(Some("blockId1"), Some(1))),
      includedBlock = Some(BlockIndex(Some("blockId2"), Some(2))),
      status = TransferStatus.Confirming
    )))
  }

  def sendHeartBlockMessage(prevId: String, prevH: Int, id: String, h: Int, address: String = "outputAddr1", sigId: String = "mockSigId1") = {
    Client.backend ! BitwayMessage(Btc, blockMsg = Some(CryptoCurrencyBlockMessage(
      block = CryptoCurrencyBlock(BlockIndex(Some(id), Some(h)), BlockIndex(Some(prevId), Some(prevH)),
        txs = List(CryptoCurrencyTransaction(
          sigId = Some(sigId),
          ids = Some(List(1425L)),
          inputs = Some(List(CryptoCurrencyTransactionPort("inputAddr1", Some(1.0)))),
          outputs = Some(List(CryptoCurrencyTransactionPort(address, Some(1.0)))),
          status = TransferStatus.Confirming
        )))
    )))
  }

  def main(args: Array[String]) {

    args(0) match {
      case "reg" => registerDepositUsers
      case "add" => addGofvRobots(args(1).toInt)
      case "rm" => removeGofvRobots
      case _ => println("usage: \nClient add 100    -- add 100 robots\nrm    -- remove all robots")
    }
  }
}
