/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.fee

import com.coinport.coinex.data.TFeeRule

sealed trait FeeRule {
  def getFee(amount: Long): Long
  def toThrift: TFeeRule
}

case class PercentageFee(percentage: Double = 0.0) extends FeeRule {
  def getFee(amount: Long) = (amount * percentage).round

  def toThrift = TFeeRule(percentage = Some(percentage))
}

case class ConstantFee(fee: Long) extends FeeRule {
  def getFee(amount: Long) = fee

  def toThrift = TFeeRule(fee = Some(fee))
}
