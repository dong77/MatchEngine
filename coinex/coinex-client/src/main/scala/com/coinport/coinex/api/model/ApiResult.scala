/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.model

case class ApiResult(success: Boolean = true, code: Int = 0, message: String = "", data: Option[Any] = None)

case class ApiSubmitOrderResult(order: ApiOrder)

case class ApiAccountItem(currency: String, available: CurrencyObject, locked: CurrencyObject, pendingWithdrawal: CurrencyObject, total: CurrencyObject)

case class ApiUserAccount(uid: String, accounts: Map[String, ApiAccountItem] = Map())

case class ApiMarketDepthItem(price: PriceObject, amount: CurrencyObject)

case class ApiMarketDepth(bids: Seq[ApiMarketDepthItem], asks: Seq[ApiMarketDepthItem])

case class ApiTicker(market: String, price: PriceObject, high: PriceObject, low: PriceObject, volume: CurrencyObject, gain: Option[Double] = None, trend: Option[String] = None)

case class ApiTransaction(id: String, timestamp: Long, price: PriceObject, subjectAmount: CurrencyObject, currencyAmount: CurrencyObject, maker: String, taker: String, sell: Boolean, tOrder: ApiOrderState, mOrder: ApiOrderState)

case class ApiOrderState(oid: String, uid: String, preAmount: CurrencyObject, curAmount: CurrencyObject)

case class ApiAssetItem(uid: String, assetMap: Map[String, CurrencyObject], amountMap: Map[String, CurrencyObject], priceMap: Map[String, PriceObject], timestamp: Long)

case class ApiTransferItem(id: String, uid: String, amount: CurrencyObject, status: Int, created: Long, updated: Long, operation: Int, address: String, txid: String)

case class ApiPagingWrapper(skip: Int, limit: Int, items: Any, count: Int)

case class ApiCandleItem(time: Long, open: PriceObject, high: PriceObject, low: PriceObject, close: PriceObject, outAmount: CurrencyObject)

case class ApiMAItem(time: Long, value: PriceObject)

case class ApiHistory(candles: Seq[ApiCandleItem])

case class ApiNotification(id: Long, ntype: String, title: String, content: String, created: Long, updated: Long, removed: Boolean, lang: String)

case class ApiNetworkStatus(currency: String, timestamp: Long, delay: Long, height: Option[Long], block: Option[String])

case class ApiWallet(currency: String, address: String, amount: CurrencyObject, accumulated: CurrencyObject, walletType: String, lastTx: Option[String], height: Option[Long])

case class ApiActorsPath(ip: String, actors: Seq[String])

case class ApiActorsInfo(pathList: Seq[ApiActorsPath])

case class ApiCurrencyReserve(available: CurrencyObject, total: CurrencyObject)

