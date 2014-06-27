/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.debug

import com.coinport.coinex.data._
import Implicits._

object Debugger {

  def prettyOutput(side: MarketSide, state: TMarketState): String = {
    val sb = new StringBuilder("========================= MarketState ==========================\n")
    sb.append("Seller Orders\n")
    val sellList = state.orderPools.getOrElse(side, List.empty[Order])
    sellList.reverse foreach { order =>
      sb.append(prettyOutput(order, true))
    }
    sb.append("################################################################\n")
    sb.append("Buyer Orders\n")
    val buyList = state.orderPools.getOrElse(side.reverse, List.empty[Order])
    buyList foreach { order =>
      sb.append(prettyOutput(order, false))
    }
    sb.append("================================================================\n")
    sb.toString
  }

  def prettyOutput(order: Order, isSell: Boolean): String = {
    val sb = new StringBuilder()
    sb.append("user: %d; price: %f; quantity: %d   ----   Order: %s\n".format(order.userId,
      if (isSell) order.price.get.value else order.price.get.reciprocal.value,
      if (isSell) order.quantity else (order.price.get * order.quantity).toLong, order))
    sb.toString
  }

  def prettyOutput(side: MarketSide, os: OrderSubmitted): String = {
    val sb = new StringBuilder()
    val isSell = side == os.originOrderInfo.side
    os.txs foreach { tx =>
      val Transaction(_, _, _, takerUpdate, makerUpdate, _) = tx
      val price = makerUpdate.previous.price.get
      sb.append("User %d %s %d %s %d. Price: %f %s: %d\n".format(
        takerUpdate.previous.userId, if (isSell) "sold" else "bought",
        if (isSell) takerUpdate.previous.quantity - takerUpdate.current.quantity else
          (takerUpdate.current.inAmount - takerUpdate.previous.inAmount).toLong,
        if (isSell) "to" else "from",
        makerUpdate.previous.userId, if (isSell) price.reciprocal.value else price.value,
        if (isSell) "received" else "paid",
        if (isSell) makerUpdate.previous.quantity - makerUpdate.current.quantity else
          (makerUpdate.current.inAmount - makerUpdate.previous.inAmount).toLong))
    }
    sb.toString
  }
}
