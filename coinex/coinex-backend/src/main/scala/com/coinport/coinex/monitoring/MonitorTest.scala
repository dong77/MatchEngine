package com.coinport.coinex.monitoring

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.cluster.Cluster

object MonitorTest {

  val configPath = System.getProperty("akka.config") match {
    case null => "akka.conf"
    case c => c
  }
  private val config = ConfigFactory.load(configPath)
  private implicit val system = ActorSystem("coinex", config)
  private implicit val cluster = Cluster(system)

  def killActor(path: String) {
    val f = cluster.system.actorSelection(path).resolveOne(2 seconds)
    f onSuccess {
      case m => {
        system.stop(m)
      }
    }
    f onFailure { case m => }
  }
}