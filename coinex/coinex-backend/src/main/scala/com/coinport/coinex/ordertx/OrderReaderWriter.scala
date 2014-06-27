package com.coinport.coinex.ordertx

import com.coinport.coinex.data._
import com.mongodb.casbah.MongoDB
import akka.actor.{ ActorLogging, Actor }
import akka.event.LoggingReceive
import Implicits._
import com.coinport.coinex.common.ExtendedView
import com.coinport.coinex.common.PersistentId._
import akka.persistence.Persistent
import com.coinport.coinex.common.ExtendedActor

class OrderReader(db: MongoDB) extends ExtendedActor with OrderMongoHandler with ActorLogging {
  val coll = db("orders")

  def receive = LoggingReceive {
    case q: QueryOrder =>
      sender ! QueryOrderResult(getItems(q), countItems(q))
  }
}

class OrderWriter(db: MongoDB) extends ExtendedView with OrderMongoHandler with ActorLogging {
  val processorId = MARKET_UPDATE_PROCESSOR <<
  val coll = db("orders")

  def receive = LoggingReceive {
    case Persistent(OrderCancelled(side, order), _) => cancelItem(order.id)

    case e @ Persistent(OrderSubmitted(orderInfo, txs), _) =>
      txs.foreach {
        tx =>
          val quantity = tx.makerUpdate.current.quantity
          val inAmount = tx.makerUpdate.current.inAmount
          val status =
            if (!tx.makerUpdate.current.canBecomeMaker) OrderStatus.FullyExecuted
            else OrderStatus.PartiallyExecuted

          updateItem(tx.makerUpdate.current.id, inAmount, quantity, status.getValue(), orderInfo.side.reverse,
            tx.timestamp, tx.makerUpdate.current.refund)
      }

      addItem(orderInfo, if (txs.isEmpty) orderInfo.order.quantity else txs.last.takerUpdate.current.quantity)
  }
}
