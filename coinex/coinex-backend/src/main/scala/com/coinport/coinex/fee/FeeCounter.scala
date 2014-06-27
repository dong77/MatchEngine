/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.fee

import com.coinport.coinex.data.Fee
import com.coinport.coinex.data._
import Implicits._

final class FeeCounter(feeConfig: FeeConfig) {
  def count(event: Any): Seq[Fee] = if (countFee.isDefinedAt(event)) countFee(event) else Nil

  val countFee: PartialFunction[Any, Seq[Fee]] = {
    case tx: Transaction =>
      val (takerInAmount, makerInAmount) = (tx.makerUpdate.outAmount, tx.takerUpdate.outAmount)
      var (takerFee, makerFee) = (0L, 0L)

      if (tx.takerUpdate.userId > feeConfig.freeOfTxChargeUserIdThreshold) {
        feeConfig.marketFeeRules.get(tx.side) foreach { rule =>
          takerFee += rule.getFee(takerInAmount)
        }

        for {
          robotType <- tx.takerUpdate.current.robotType
          rule <- feeConfig.robotFeeRules.get(robotType)
        } { takerFee += rule.getFee(takerInAmount) }
      }

      if (tx.makerUpdate.userId > feeConfig.freeOfTxChargeUserIdThreshold) {
        feeConfig.marketFeeRules.get(tx.side) foreach { rule =>
          makerFee += rule.getFee(makerInAmount)
        }

        for {
          robotType <- tx.makerUpdate.current.robotType
          rule <- feeConfig.robotFeeRules.get(robotType)
        } { makerFee += rule.getFee(makerInAmount) }
      }

      val result = Seq(Fee(tx.makerUpdate.current.userId, None, tx.side.outCurrency, makerFee),
        Fee(tx.takerUpdate.current.userId, None, tx.side.inCurrency, takerFee))

      result.filter(_.amount > 0)

    case t: AccountTransfer if t.`type` == TransferType.Withdrawal =>
      feeConfig.transferFeeRules.get(t.currency) match {
        case Some(rule) =>
          Seq(Fee(t.userId, None, t.currency, rule.getFee(t.amount)))
        case None =>
          Nil
      }
  }
}
