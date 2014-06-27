package com.coinport.coinex.common.support

import akka.actor.Actor
import akka.actor.Cancellable
import scala.concurrent.duration._
import com.coinport.coinex.data.TakeSnapshotNow
import akka.actor.ActorLogging
import java.io.FileOutputStream
import akka.serialization.SerializationExtension
import com.mongodb.util.JSON
import com.coinport.coinex.serializers._
import ThriftEnumJson4sSerialization._
import java.text.SimpleDateFormat
import java.util.Date
import akka.actor.ActorPath
import org.json4s._
import org.json4s.native.JsonMethods._

trait SnapshotSupport extends Actor with ActorLogging {
  implicit val formats = ThriftEnumJson4sSerialization.formats + MapSerializer
  private var cancellable: Cancellable = null
  val snapshotIntervalSec: Int

  abstract override def preStart() = {
    super.preStart()
    val initialDelaySec = timeInSecondsToNextHalfHour()
    // val initialDelaySec = 60
    scheduleSnapshot(initialDelaySec, TakeSnapshotNow("auto", Some(snapshotIntervalSec)))
    log.info(s"next snapshot will be taken in ${initialDelaySec} seconds, then every ${snapshotIntervalSec} seconds")
  }

  def takeSnapshot(cmd: TakeSnapshotNow)(action: => Unit) = {
    cancelSnapshotSchedule()
    action

    if (cmd.nextSnapshotinSeconds.isDefined && cmd.nextSnapshotinSeconds.get > 0) {
      scheduleSnapshot(cmd.nextSnapshotinSeconds.get, cmd)
      log.info(s"a new snapshot was taken, next snapshot will be taken in ${cmd.nextSnapshotinSeconds.get / 60} minutes")
    } else {
      log.info("a new snapshot was taken, no snapshot is scheduled")
    }
  }

  protected def cancelSnapshotSchedule() =
    if (cancellable != null && !cancellable.isCancelled) cancellable.cancel()

  protected def scheduleSnapshot(delayinSeconds: Int, cmd: TakeSnapshotNow) =
    cancellable = context.system.scheduler.scheduleOnce(delayinSeconds seconds, self, cmd)(context.system.dispatcher)

  private def timeInSecondsToNextHalfHour() = {
    val time = System.currentTimeMillis()
    (((time / 1800000 + 1) * 1800000 - time) / 1000).toInt
  }

  def dumpToFile(state: AnyRef, actorPath: ActorPath): String = {
    val fileName = "/tmp/" + actorPath.toString.replace("akka://coinex/user/", "").replace("/", "__") +
      (new SimpleDateFormat("~yyyy_MM_dd_HH_mm_ss").format(new Date())) + ".json"
    val out = new FileOutputStream(fileName)
    try {
      val filtered = Extraction.decompose(state).removeField {
        case JField("_passthroughFields", _) => true
        case _ => false
      }
      out.write(pretty(render(filtered)).getBytes)
      log.info(s"state of type ${state.getClass.getName} dumped to file ${fileName}")
    } catch {
      case e: Throwable =>
        log.error(s"Unable to dump state of type ${state.getClass.getName} to file ${fileName}", e.getStackTraceString)
        throw e
    } finally {
      out.close()
    }
    fileName
  }
}
