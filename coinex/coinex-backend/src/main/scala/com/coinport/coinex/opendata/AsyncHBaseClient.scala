package com.coinport.coinex.opendata

import org.hbase.async.HBaseClient
import akka.persistence.hbase.journal.{ PluginPersistenceSettings, HBaseClientFactory }
import akka.actor.ExtendedActorSystem
import akka.persistence.PersistenceSettings

class AsyncHBaseClient(implicit val system: ExtendedActorSystem) {
  private val config = system.settings.config
  // use journal config as hbse client config
  private val hBasePersistenceSettings = PluginPersistenceSettings(config, "hbase-journal")

  def getClient(): HBaseClient = {
    HBaseClientFactory.getClient(hBasePersistenceSettings, new PersistenceSettings(config.getConfig("akka.persistence")))
  }

  def shutDown() {
    HBaseClientFactory.shutDown()
  }
}