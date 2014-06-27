/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.routing._
import akka.contrib.pattern.ClusterSingletonManager
import akka.io.IO
import akka.routing._
import com.typesafe.config.Config
import org.slf4s.Logging
import org.apache.commons.io.IOUtils
import scala.collection.mutable.ListBuffer
import spray.can.Http
import com.coinport.coinex.accounts._
import com.coinport.coinex.apiauth._
import com.coinport.coinex.common._
import com.coinport.coinex.common.stackable._
import com.coinport.coinex.data._
import com.coinport.coinex.mail._
import com.coinport.coinex.markets._
import com.coinport.coinex.metrics._
import com.coinport.coinex.monitoring._
import com.coinport.coinex.opendata._
import com.coinport.coinex.robot._
import com.coinport.coinex.ordertx._
import com.coinport.coinex.users._
import com.coinport.coinex.transfer._
import com.coinport.coinex.util._
import com.coinport.coinex.bitway._
import Implicits._
import scala.collection.mutable.ListBuffer
import com.coinport.coinex.common._
import ConstantRole._
import MarketRole._
import BitwayRole._
import com.mongodb.casbah._

import com.twitter.util.Eval
import java.io.File
import java.io.InputStream
import com.coinport.coinex.admin.NotificationReaderWriter

class Deployer(config: Config, hostname: String, markets: Seq[MarketSide])(implicit cluster: Cluster) extends Object with Logging {
  implicit val system = cluster.system
  val paths = new ListBuffer[String]
  val allPaths = new ListBuffer[String]
  val secret = config.getString("akka.exchange.secret")
  val userManagerSecret = MHash.sha256Base64(secret + "userProcessorSecret")
  val apiAuthSecret = MHash.sha256Base64(secret + "apiAuthSecret")

  val mongoUriForViews = MongoURI(config.getString("akka.exchange.mongo-uri-for-readers"))
  val mongoForViews = MongoConnection(mongoUriForViews)
  val dbForViews = mongoForViews(mongoUriForViews.database.get)

  val mongoUriForEventExport = MongoURI(config.getString("akka.exchange.mongo-uri-for-events"))
  val mongoForEventExport = MongoConnection(mongoUriForEventExport)
  val dbForEventExport = mongoForEventExport(mongoUriForEventExport.database.get)
  val asyncHBaseClient = new AsyncHBaseClient()

  def shutdown() {
    mongoForViews.close()
    mongoForEventExport.close()
    asyncHBaseClient.shutDown()
  }

  def deploy(): LocalRouters = {
    val accountConfig = loadConfig[AccountConfig](config.getString("akka.exchange.account-config-path"))

    deployMailer(mailer <<)

    // Deploy views first
    markets foreach { m =>
      deploy(Props(new MarketDepthView(m) with StackableView[TMarketState, MarketManager]), market_depth_view << m)
      deploy(Props(new CandleDataView(m) with StackableView[TCandleDataState, CandleDataManager]), candle_data_view << m)
    }

    deploy(Props(new UserWriter(dbForViews, userManagerSecret)), user_mongo_writer <<)
    deploy(Props(new AccountView(accountConfig) with StackableView[TAccountState, AccountManager]), account_view <<)
    deploy(Props(new AssetView with StackableView[TAssetState, AssetManager]), asset_view <<)
    deploy(Props(new MetricsView with StackableView[TMetricsState, MetricsManager]), metrics_view <<)
    deploy(Props(new ApiAuthView(apiAuthSecret) with StackableView[TApiSecretState, ApiAuthManager]), api_auth_view <<)

    deploy(Props(new TransactionReader(dbForViews)), transaction_mongo_reader <<)
    deploy(Props(new OrderReader(dbForViews)), order_mongo_reader <<)
    deploy(Props(new AccountTransferReader(dbForViews)), account_transfer_mongo_reader <<)
    deploy(Props(new NotificationReaderWriter(dbForViews)), notification_mongo <<)

    // Then deploy routers
    val routers = new LocalRouters(markets)

    // Finally deploy processors
    deploySingleton(Props(new AccountProcessor(
      routers.marketProcessors,
      routers.marketUpdateProcessor.path,
      routers.depositWithdrawProcessor.path,
      accountConfig) with StackableEventsourced[TAccountState, AccountManager]), account_processor <<)

    markets foreach { m =>
      def props = Props(new MarketProcessor(m,
        routers.accountProcessor.path) with StackableEventsourced[TMarketState, MarketManager])
      deploySingleton(props, market_processor << m)
    }

    deploySingleton(Props(new MarketUpdateProcessor() with StackableCmdsourced[TSimpleState, SimpleManager]), market_update_processor <<)
    deploySingleton(Props(new UserProcessor(routers.mailer, userManagerSecret) with StackableEventsourced[TUserState, UserManager]), user_processor <<)
    deploySingleton(Props(new ApiAuthProcessor(apiAuthSecret) with StackableCmdsourced[TApiSecretState, ApiAuthManager]), api_auth_processor <<)
    deploySingleton(Props(new RobotProcessor(routers) with StackableCmdsourced[TRobotState, RobotManager]), robot_processor <<)
    deploySingleton(Props(new AccountTransferProcessor(dbForViews, routers.accountProcessor.path, routers.bitwayProcessors) with StackableEventsourced[TAccountTransferState, AccountTransferManager]), account_transfer_processor <<)

    deploySingleton(Props(new TransactionWriter(dbForViews)), transaction_mongo_writer <<)
    deploySingleton(Props(new OrderWriter(dbForViews)), order_mongo_writer <<)
    deploySingleton(Props(new ExportOpenDataProcessor(asyncHBaseClient) with StackableEventsourced[ExportOpenDataMap, ExportOpenDataManager]), opendata_exporter <<)

    val configs = loadConfig[BitwayConfigs](config.getString("akka.exchange.bitway-path")).configs
    markets.toCryptoCurrencySet foreach { c =>
      def props = Props(new BitwayProcessor(routers.depositWithdrawProcessor,
        c, configs.getOrElse(c, BitwayConfig())) with StackableEventsourced[TBitwayState, BitwayManager])
      deploySingleton(props, bitway_processor << c)
      deploy(Props(new BitwayView(c, configs.getOrElse(c, BitwayConfig())) with StackableView[TBitwayState, BitwayManager]), bitway_view << c)
      deploy(Props(new BitwayReceiver(routers.bitwayProcessors(c), c, configs.getOrElse(c, BitwayConfig()))), bitway_receiver << c)
    }

    // Deploy monitor at last
    deployMonitor(routers)

    routers
  }

  private def deploySingleton(props: => Props, name: String) = {
    allPaths += name + "/singleton"
    if (cluster.selfRoles.contains(name)) {
      val actor = system.actorOf(ClusterSingletonManager.props(
        singletonProps = props,
        singletonName = "singleton",
        terminationMessage = PoisonPill,
        role = Some(name)),
        name = name)
      paths += actor.path.toStringWithoutAddress + "/singleton"
    }
  }

  private def deploy(props: => Props, name: String) = {
    allPaths += name
    if (cluster.selfRoles.contains(name)) {
      val actor = system.actorOf(props, name)
      paths += actor.path.toStringWithoutAddress
    }
  }

  private def deployMailer(name: String) = {
    allPaths += name
    if (cluster.selfRoles.contains(name)) {
      val mandrilApiKey = config.getString("akka.exchange.mailer.mandrill-api-key")
      val handler = new MandrillMailHandler(mandrilApiKey)
      val props = Props(new Mailer(handler))
      val actor = system.actorOf(FromConfig.props(props), name)
      paths += actor.path.toStringWithoutAddress
    }
  }

  private def deployMonitor(routers: LocalRouters) = {
    if (cluster.selfRoles.contains(monitor_service <<)) {
      val service = system.actorOf(ClusterSingletonManager.props(
        singletonProps = Props(new Monitor(paths.toList, routers.mailer, config: Config, allPaths.toList)),
        singletonName = "singleton",
        terminationMessage = PoisonPill,
        role = Some(monitor_service <<)),
        name = monitor_service <<)
      val port = config.getInt("akka.exchange.monitor.http-port")
      IO(Http) ! Http.Bind(service, hostname, port)
      log.info("Started HTTP server: http://" + hostname + ":" + port)
    }
  }

  private def loadConfig[T](configPath: String): T = {
    val in: InputStream = this.getClass.getClassLoader.getResourceAsStream(configPath)
    (new Eval()(IOUtils.toString(in))).asInstanceOf[T]
  }
}
