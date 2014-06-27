package com.coinport.coinex.common.stackable

import akka.persistence.View
import akka.persistence.SnapshotOffer
import akka.actor.ActorLogging
import com.coinport.coinex.data._
import com.coinport.coinex.common.Manager
import com.coinport.coinex.common.support._

trait StackableView[T <: AnyRef, M <: Manager[T]]
    extends View with ActorLogging with SnapshotSupport {
  val manager: M

  abstract override def receive = super.receive orElse {
    case cmd: TakeSnapshotNow => takeSnapshot(cmd)(saveSnapshot(manager.getSnapshot))

    case SnapshotOffer(meta, snapshot) =>
      log.info("Loading snapshot: " + meta)
      manager.loadSnapshot(snapshot.asInstanceOf[T])

    case DumpStateToFile(_) => sender ! dumpToFile(manager.getSnapshot, self.path)
  }
}
