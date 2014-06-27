package com.coinport.coinex.common.support

import akka.actor.Actor
import akka.persistence._
import akka.persistence.ConfirmablePersistent
import scala.concurrent.duration._

trait ChannelSupport { self: Actor =>
  def processorId: String

  protected def createChannelTo(dest: String) = {
    val channelName = processorId + "_2_" + dest
    context.actorOf(Channel.props(channelName, ChannelSettings(redeliverInterval = 30 seconds)), channelName)
  }

  protected def createPersistentChannelTo(dest: String) = {
    val channelName = processorId + "_2_" + dest
    context.actorOf(PersistentChannel.props(channelName, PersistentChannelSettings(redeliverInterval = 30 seconds)), channelName)
  }

  def confirm(p: ConfirmablePersistent): Unit
}
