/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex

import akka.actor._
import akka.cluster.Cluster
import akka.pattern.ask
import akka.persistence._
import akka.util.Timeout
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.concurrent.duration._
import scala.io.Source

import com.coinport.coinex.common._
import com.coinport.coinex.data._
import Currency._
import Implicits._

object CoinexApp extends App {
  val markets = Seq(Ltc ~> Btc, Doge ~> Btc)
  val allRoles = (ConstantRole.values.map(_.<<) ++ MarketRole.values.map { v => markets.map { m => v << m } }.flatten ++
    BitwayRole.values.map { v => markets.toCryptoCurrencySet.map { c => v << c } }.flatten)

  if (args.length < 2 || args.length > 5) {
    val message = """please supply 1 to 5 parameters:
        required args(0): port - supply 0 to select a port randomly
        required args(1): seeds - seed note seperated by comma, i.e, "127.0.0.1:25551,127.0.0.1:25552"
        optioanl args(2): roles - "*" for all roles, "" for empty node, and "a,b,c" for 3 roles
        optioanl args(3): hostname - self hostname
        optioanl args(4): prev-config - private config

        available roles:%s
      """.format(allRoles.mkString("\n\t\t", "\n\t\t", "\n\t\t"))
    println(message)
    System.exit(1)
  }

  val seeds = args(1).split(",").map(_.stripMargin).filter(_.nonEmpty).map("\"akka.tcp://coinex@" + _ + "\"").mkString(",")

  val roles =
    if (args.length < 3) ""
    else if (args(2) == "*" || args(2) == "all") allRoles.mkString(",")
    else args(2).split(",").map(_.stripMargin).filter(_.nonEmpty).map("\"" + _ + "\"").mkString(",")

  val hostname =
    if (args.length < 4) InetAddress.getLocalHost.getHostAddress
    else args(3)

  val prevConfigFile: Option[String] = if (args.length < 5) None else Some(args(4))

  var config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0))
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + hostname))
    .withFallback(ConfigFactory.parseString("akka.cluster.roles=[" + roles + "]"))
    .withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[" + seeds + "]"))

  if (!prevConfigFile.isDefined) {
    println("No private config file found!")
    println("private config like this:\n\takka.persistence.encryption-settings=x\n\takka.mongo.username=x\n\takka.mongo.password=x")
    System.exit(1)
  } else {
    for (line <- Source.fromFile(prevConfigFile.get).getLines) {
      // val List(k, v) = line.split("=").map(i => i.trim).toList
      config = config.withFallback(ConfigFactory.parseString(line))
    }
  }

  config = config.withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("coinex", config)
  implicit val cluster = Cluster(system)

  val routers = new Deployer(config, hostname, markets).deploy()

  Thread.sleep(5000)
  val summary = "============= Akka Node Ready =============\n" +
    "with hostname: " + hostname + "\n" +
    "with seeds: " + seeds + "\n" +
    "with roles: " + roles + "\n"

  println(summary)
  val coinport = """
                                            _
             (_)                           | |
   ___  ___   _  _ __   _ __    ___   _ __ | |_ __  __
  / __|/ _ \ | || '_ \ | '_ \  / _ \ | '__|| __|\ \/ /
 | (__| (_) || || | | || |_) || (_) || |   | |_  >  <
  \___|\___/ |_||_| |_|| .__/  \___/ |_|    \__|/_/\_\
                       | |
                       |_|
"""
  println(coinport)
}

