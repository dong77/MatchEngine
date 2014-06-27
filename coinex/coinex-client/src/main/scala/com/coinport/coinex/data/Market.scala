package com.coinport.coinex.data

case class Market(currency1: Currency, currency2: Currency) {
  val sorted = if (currency1.getValue > currency2.getValue) (currency1, currency2) else (currency2, currency1)

  def getMarketSide(ordered: Boolean = true) = if (ordered) MarketSide(sorted._1, sorted._2) else MarketSide(sorted._2, sorted._1)

  override def hashCode = sorted.hashCode

  override def toString = (sorted._1.toString + "-" + sorted._2.toString).toUpperCase

  override def equals(obj: Any): Boolean = {
    obj.isInstanceOf[Market] && {
      val that = obj.asInstanceOf[Market]
      (currency1, currency2) == (that.currency1, that.currency2) || (currency2, currency1) == (that.currency1, that.currency2)
    }
  }
}