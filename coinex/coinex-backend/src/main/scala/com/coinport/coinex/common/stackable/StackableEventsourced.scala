package com.coinport.coinex.common.stackable

import akka.actor.ActorLogging
import akka.actor.Actor.Receive
import akka.persistence.EventsourcedProcessor
import akka.persistence.SnapshotOffer

import com.coinport.coinex.common.Manager
import com.coinport.coinex.common.support._
import com.coinport.coinex.data._

trait StackableEventsourced[T <: AnyRef, M <: Manager[T]]
    extends EventsourcedProcessor with ActorLogging with SnapshotSupport with RecoverySupport with RedeliverFilterSupport[T, M] {
  val manager: M
  def updateState: Receive

  abstract override def receiveRecover = super.receiveRecover orElse {
    case SnapshotOffer(meta, snapshot) =>
      log.info("Loading snapshot: " + meta)
      manager.loadSnapshot(snapshot.asInstanceOf[T])

    case event: AnyRef => updateState(event)
  }

  abstract override def receiveCommand = filterFor(super.receiveCommand) orElse super.receiveCommand orElse {
    case cmd: TakeSnapshotNow => takeSnapshot(cmd)(saveSnapshot(manager.getSnapshot))
    case QueryRecoverStats => execAfterRecover(recoveryFinished)
    case DumpStateToFile(_) => sender ! dumpToFile(manager.getSnapshot, self.path)
  }
}
