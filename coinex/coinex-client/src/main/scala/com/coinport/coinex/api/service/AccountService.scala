/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.service

import com.coinport.coinex.data._
import com.coinport.coinex.api.model._
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._

object AccountService extends AkkaService {
  def getAccount(uid: Long): Future[ApiResult] = {
    backend ? QueryAccount(uid) map {
      case result: QueryAccountResult =>
        ApiResult(true, 0, "", Some(fromUserAccount(result.userAccount)))
    }
  }

  // TODO: remove this function
  def deposit(uid: Long, currency: Currency, amount: Double): Future[ApiResult] = {
    val internalAmount: Long = amount.internalValue(currency)

    val deposit = AccountTransfer(0L, uid.toLong, TransferType.Deposit, currency, internalAmount, TransferStatus.Pending)
    backend ? DoRequestTransfer(deposit) map {
      case result: RequestTransferSucceeded =>

        backend ! AdminConfirmTransferSuccess(result.transfer)

        ApiResult(true, 0, "充值申请已提交", Some(result))
      case failed: RequestTransferFailed =>
        ApiResult(false, 1, "充值失败", Some(failed))
    }
  }

  def withdrawal(uid: Long, currency: Currency, amount: Double, address: String): Future[ApiResult] = {
    val internalAmount: Long = amount.internalValue(currency)

    val withdrawal = AccountTransfer(0L, uid.toLong, TransferType.Withdrawal, currency, internalAmount, TransferStatus.Pending, address = Some(address))
    backend ? DoRequestTransfer(withdrawal) map {
      case result: RequestTransferSucceeded =>
        ApiResult(true, 0, "提现申请已提交", Some(result))
      case failed: RequestTransferFailed =>
        ApiResult(false, 1, "提现失败", Some(failed))
    }
  }

  def submitOrder(userOrder: UserOrder): Future[ApiResult] = {
    val command = userOrder.toDoSubmitOrder
    backend ? command map {
      case result: OrderSubmitted =>
        ApiResult(true, 0, "订单提交成功", Some(ApiOrder.fromOrderInfo(result.originOrderInfo)))
      case failed: SubmitOrderFailed =>
        val message = failed.error match {
          case ErrorCode.InsufficientFund => "余额不足"
          case error => "未知错误-" + error
        }
        ApiResult(false, failed.error.getValue, message)
      case x =>
        ApiResult(false, -1, x.toString)
    }
  }

  def cancelOrder(id: Long, uid: Long, side: MarketSide): Future[ApiResult] = {
    backend ? DoCancelOrder(side, id, uid) map {
      case result: OrderCancelled => ApiResult(true, 0, "订单已撤销", Some(result.order))
      case x => ApiResult(false, -1, x.toString)
    }
  }

  def getOrders(marketSide: Option[MarketSide], uid: Option[Long], id: Option[Long], status: Option[OrderStatus], skip: Int, limit: Int): Future[ApiResult] = {
    val cursor = Cursor(skip, limit)
    val querySide = marketSide.map(side => QueryMarketSide(side, true))
    backend ? QueryOrder(uid, id, status.map(_.getValue), querySide, cursor) map {
      case result: QueryOrderResult =>
        val items = result.orderinfos.map {
          o => ApiOrder.fromOrderInfo(o)
        }.toSeq
        ApiResult(data = Some(ApiPagingWrapper(skip, limit, items, result.count.toInt)))
      case x => ApiResult(false, -1, x.toString)
    }
  }
}
