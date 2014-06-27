package com.coinport.coinex.api.service

import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

object BitwayService extends AkkaService {
  def getNetworkStatus(currency: Currency) = {
    backend ? QueryCryptoCurrencyNetworkStatus(currency) map {
      case result: QueryCryptoCurrencyNetworkStatusResult =>
        val timestamp = result.status.heartbeatTime.getOrElse(0L)
        val current = result.status.queryTimestamp.getOrElse(0L)
        val delay = if (timestamp == 0 || current == 0) -1L else (current - timestamp max 0L)

        val data = ApiNetworkStatus(
          currency = result.currency,
          timestamp = timestamp,
          delay = delay,
          height = result.status.height,
          block = result.status.id
        )
        ApiResult(data = Some(data))
      case r =>
        ApiResult(false, -1, "unknown result", Some(r))
    }
  }

  def getWallets(currency: Currency, addressType: CryptoCurrencyAddressType) = {
    backend ? QueryCryptoCurrencyAddressStatus(currency, addressType) map {
      case result: QueryCryptoCurrencyAddressStatusResult =>
        var accumulatedAmount = 0L
        val data = result.status.map {
          case kv =>
            accumulatedAmount += kv._2.confirmedAmount
            ApiWallet(
              currency = currency,
              address = kv._1,
              amount = CurrencyObject(currency, kv._2.confirmedAmount),
              accumulated = CurrencyObject(currency, accumulatedAmount),
              walletType = addressType.toString.toLowerCase,
              lastTx = kv._2.txid,
              height = kv._2.height
            )
        }
        ApiResult(data = Some(data))
      case r =>
        ApiResult(false, -1, "unknown result", Some(r))
    }
  }
}
