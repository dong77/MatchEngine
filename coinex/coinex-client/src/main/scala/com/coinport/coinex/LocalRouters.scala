/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex

import akka.actor._
import akka.cluster.routing._
import akka.routing._
import akka.contrib.pattern.ClusterSingletonProxy
import com.coinport.coinex.data._
import Implicits._
import com.coinport.coinex.common._
import ConstantRole._
import MarketRole._
import BitwayRole._
import akka.cluster.Cluster

class LocalRouters(markets: Seq[MarketSide])(implicit cluster: Cluster) {
  implicit val system = cluster.system

  val userProcessor = routerForSingleton(user_processor <<)
  val accountProcessor = routerForSingleton(account_processor <<)
  val marketUpdateProcessor = routerForSingleton(market_update_processor <<)
  val apiAuthProcessor = routerForSingleton(api_auth_processor <<)

  val marketProcessors = bidirection(Map(markets map { m =>
    m -> routerForSingleton(market_processor << m)
  }: _*))

  val robotProcessor = routerForSingleton(robot_processor<<)
  val depositWithdrawProcessor = routerForSingleton(account_transfer_processor <<)

  val accountView = routerFor(account_view <<)
  val apiAuthView = routerFor(api_auth_view <<)

  val assetView = routerFor(asset_view <<)

  val candleDataView = bidirection(Map(markets map { m =>
    m -> routerFor(candle_data_view << m)
  }: _*))

  val marketDepthViews = bidirection(Map(markets map { m =>
    m -> routerFor(market_depth_view << m)
  }: _*))

  val transactionReader = routerFor(transaction_mongo_reader <<)
  val transactionWriter = routerFor(transaction_mongo_writer <<)

  val orderReader = routerFor(order_mongo_reader <<)
  val orderWriter = routerFor(order_mongo_writer <<)

  val depositWithdrawReader = routerFor(account_transfer_mongo_reader <<)

  val mailer = routerFor(ConstantRole.mailer <<)

  val bitwayProcessors = Map(markets.toCryptoCurrencySet map (c =>
    c -> routerForSingleton(bitway_processor << c)
  ): _*)

  val bitwayViews = Map(markets.toCryptoCurrencySet map (c =>
    c -> routerFor(bitway_view << c)
  ): _*)

  val notification = routerFor(notification_mongo <<)

  val metricsView = routerFor(metrics_view<<)

  val monitorService = routerForSingleton(monitor_service <<)

  private def routerForSingleton(name: String) = system.actorOf(
    ClusterSingletonProxy.defaultProps("/user/" + name + "/singleton", name),
    name + "_router")

  private def routerFor(name: String) = system.actorOf(
    ClusterRouterGroup(RoundRobinGroup(Nil),
      ClusterRouterGroupSettings(
        totalInstances = Int.MaxValue,
        routeesPaths = List("/user/" + name),
        allowLocalRoutees = cluster.selfRoles.contains(name),
        useRole = Some(name))).props,
    name + "_router")

  private def bidirection(m: Map[MarketSide, ActorRef]): Map[MarketSide, ActorRef] = {
    m ++ m.map {
      case (side, v) => (side.reverse, v)
    }
  }
}
