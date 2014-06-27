package com.coinport.coinex.opendata

import akka.actor.Cancellable
import akka.event.LoggingReceive
import akka.persistence.EventsourcedProcessor
import com.coinport.coinex.common.ExtendedProcessor
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.data._
import com.twitter.util.Eval
import java.io._
import org.apache.commons.io.IOUtils
import scala.collection.mutable.Map

import Implicits._

class ExportOpenDataProcessor(var asyncHBaseClient: AsyncHBaseClient) extends ExtendedProcessor
    with EventsourcedProcessor {
  override def processorId = EXPORT_OPEN_DATA_PROCESSOR <<

  private var cancellable: Cancellable = null
  lazy val openDataConfig = loadConfig(context.system.settings.config.getString("akka.exchange.opendata-path"))
  private val scheduleInterval = openDataConfig.scheduleInterval
  lazy val manager = new ExportOpenDataManager(asyncHBaseClient, context, openDataConfig)

  override def preStart(): Unit = {
    super.preStart()
    if (openDataConfig.enableExportData) {
      scheduleExport()
    }
  }

  override def receiveCommand = LoggingReceive {
    case DoExportData =>
      cancelExportSchedule
      val dumpedMap = manager.exportData()
      if (dumpedMap.nonEmpty) {
        persist(ExportOpenDataMap(dumpedMap)) { _ => }
      }
      scheduleExport()
  }

  def receiveRecover = PartialFunction.empty[Any, Unit]

  def updateState: Receive = {
    case m: ExportOpenDataMap => manager.updatePSeqMap(Map.empty[String, Long] ++ m.processorSeqMap)
  }

  private def scheduleExport() = {
    cancellable = context.system.scheduler.scheduleOnce(scheduleInterval, self, DoExportData)(context.system.dispatcher)
  }

  private def cancelExportSchedule() =
    if (cancellable != null && !cancellable.isCancelled) cancellable.cancel()

  private def loadConfig(configPath: String): OpenDataConfig = {
    val in: InputStream = this.getClass.getClassLoader.getResourceAsStream(configPath)
    (new Eval()(IOUtils.toString(in))).asInstanceOf[OpenDataConfig]
  }

}