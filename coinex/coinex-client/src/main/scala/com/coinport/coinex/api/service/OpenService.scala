package com.coinport.coinex.api.service

import com.coinport.coinex.data._
import com.coinport.coinex.data.CryptoCurrencyAddressType._
import com.coinport.coinex.api.model._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

object OpenService extends AkkaService {
  def getCurrencyReserve(currency: Currency) = {
    backend ? QueryReserveStatus(currency) map {
      case rv: QueryReserveStatusResult =>
        val total = rv._2.map(_._2).sum
        val available = rv.amounts.get(Cold).getOrElse(0L) + rv.amounts.get(Hot).getOrElse(0L)
        ApiResult(data = Some(ApiCurrencyReserve(CurrencyObject(currency, available), CurrencyObject(currency, total))))
      case r =>
        ApiResult(false, -1, "unknown result", Some(r))
    }
  }
}
