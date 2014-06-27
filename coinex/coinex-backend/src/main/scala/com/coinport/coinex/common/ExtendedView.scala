/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 */

package com.coinport.coinex.common

import akka.actor.ActorLogging
import akka.persistence.View
import com.mongodb.casbah.{ MongoConnection, MongoURI, MongoCollection }
import com.coinport.coinex.common.support.SnapshotSupport

trait ExtendedView extends View with ActorLogging with SnapshotSupport with ExtendedActor {

  val snapshotIntervalSec = 60 * 60 * 24 // defalut to 24 hours
  override def preStart() = {
    log.info("------------  processorId: {}, viewId: {}", processorId, viewId)
    super.preStart
  }
}
