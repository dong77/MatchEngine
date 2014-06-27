package com.coinport.coinex.api.service

import com.typesafe.config.ConfigFactory
import com.coinport.coinex.{ Coinex, LocalRouters }
import com.coinport.coinex.data.Implicits._
import akka.actor.{ Props, ActorSystem }
import akka.cluster.Cluster
import com.coinport.coinex.data.MarketSide

object Akka {
  val defaultAkkaConfig = "akka.conf"
  val akkaConfigProp = System.getProperty("akka.config")
  val akkaConfigResource = if (akkaConfigProp != null) akkaConfigProp else defaultAkkaConfig

  println("=" * 20 + "  Akka config  " + "=" * 20)
  println("  conf/" + akkaConfigResource)
  println("=" * 55)

  val config = ConfigFactory.load(akkaConfigResource)
  implicit val system = ActorSystem("coinex", config)
  implicit val cluster = Cluster(system)

  val markets = config.getStringList("exchange.markets").toArray.map {
    m =>
      val side: MarketSide = m.asInstanceOf[String]
      println("enable market " + side.S)
      side
  }

  val routers = new LocalRouters(markets)
  val backend = system.actorOf(Props(new Coinex(routers)), name = "backend")
}
