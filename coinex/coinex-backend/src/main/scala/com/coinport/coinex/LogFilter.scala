
/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex

import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.classic.spi.ILoggingEvent

// Filter out ClusterHeartbeat messages
class LogFilter extends Filter[ILoggingEvent] {
  def decide(event: ILoggingEvent) = {
    event.getLoggerName() match {
      case "akka.cluster.ClusterHeartbeatSender" => FilterReply.DENY
      case "org.hbase.async.RegionClient" => FilterReply.DENY
      case _ => FilterReply.ACCEPT
    }
  }
}
