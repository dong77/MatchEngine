package com.coinport.coinex.ordertx

import akka.actor.{ Actor, ActorLogging }
import com.coinport.coinex.data._
import com.mongodb.casbah.Imports._
import akka.event.LoggingReceive
import com.coinport.coinex.common.ExtendedView
import com.coinport.coinex.common.PersistentId._
import Implicits._
import akka.persistence.Persistent
import com.coinport.coinex.common.ExtendedActor

class TransactionReader(val db: MongoDB) extends ExtendedActor with TransactionBehavior with ActorLogging {
  val coll = db("transactions")

  def receive = LoggingReceive {
    case q: QueryTransaction => sender ! QueryTransactionResult(getItems(q), countItems(q))
  }
}

class TransactionWriter(val db: MongoDB) extends ExtendedView with TransactionBehavior with ActorLogging {
  val processorId = MARKET_UPDATE_PROCESSOR <<
  val coll = db("transactions")

  def receive = LoggingReceive {
    case e @ Persistent(OrderSubmitted(orderInfo, txs), _) =>
      txs foreach (addItem)
  }
}
