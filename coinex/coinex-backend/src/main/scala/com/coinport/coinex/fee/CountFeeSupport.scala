/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.fee

import com.coinport.coinex.data._
import com.typesafe.config.ConfigFactory

trait CountFeeSupport {
  protected val feeConfig: FeeConfig
  private lazy val feeCounter = new FeeCounter(feeConfig)

  protected def countFee[T](event: T): T = (event match {
    case m @ OrderSubmitted(_, txs) => m.copy(txs = txs.map(tx => tx.copy(fees = Some(feeCounter.count(tx)))))
    case m: AccountTransfer => m.copy(fee = feeCounter.count(m).headOption)
    case m => m
  }).asInstanceOf[T]
}
