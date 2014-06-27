package com.coinport.coinex.transfer

import akka.actor.{ ActorLogging, Actor }
import akka.event.{ LoggingAdapter, LoggingReceive }
import com.coinport.coinex.data._
import com.mongodb.casbah.Imports._
import com.coinport.coinex.common.ExtendedActor

class AccountTransferReader(val db: MongoDB) extends ExtendedActor with AccountTransferBehavior with ActorLogging {

  lazy implicit val logger: LoggingAdapter = log

  val manager = new AccountTransferManager()

  def receive = LoggingReceive {
    case q: QueryTransfer =>
      val query = transferHandler.getQueryDBObject(q)
      val count = transferHandler.count(query)
      val items = transferHandler.find(query, q.cur.skip, q.cur.limit)
      sender ! QueryTransferResult(items, count)

    case q: QueryCryptoCurrencyTransfer =>
      val query = transferItemHandler.getQueryDBObject(q)
      val count = transferItemHandler.count(query)
      val items = transferItemHandler.find(query, q.cur.skip, q.cur.limit)
      sender ! QueryCryptoCurrencyTransferResult(items, count)
  }
}
