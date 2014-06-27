/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex

import akka.actor._
import akka.event.LoggingReceive
import akka.cluster.routing._
import akka.routing._
import com.coinport.coinex.data._
import akka.persistence._
import Implicits._
import org.slf4s.Logging

final class Coinex(routers: LocalRouters) extends Actor with Logging {

  def receive = {
    LoggingReceive {
      //-------------------------------------------------------------------------
      // User Proceessor
      case m: DoRegisterUser => routers.userProcessor forward m
      case m: DoResendVerifyEmail => routers.userProcessor forward m
      case m: DoRequestPasswordReset => routers.userProcessor forward m
      case m: DoResetPassword => routers.userProcessor forward m
      case m: VerifyEmail => routers.userProcessor forward m
      case m: Login => routers.userProcessor forward m
      case m: ValidatePasswordResetToken => routers.userProcessor forward m
      case m: VerifyGoogleAuthCode => routers.userProcessor forward m
      case m: DoUpdateUserProfile => routers.userProcessor forward m
      case m: QueryProfile => routers.userProcessor forward m
      case m: DoSuspendUser => routers.userProcessor forward m
      case m: DoResumeUser => routers.userProcessor forward m
      //-------------------------------------------------------------------------
      // Account Processor
      case m: DoRequestTransfer => routers.accountProcessor forward m
      case m: DoRequestGenerateABCode => routers.accountProcessor forward m
      case m: DoRequestACodeQuery => routers.accountProcessor forward m
      case m: DoRequestBCodeRecharge => routers.accountProcessor forward m
      case m: DoRequestConfirmRC => routers.accountProcessor forward m
      case m: DoSubmitOrder => routers.accountProcessor forward m
      case m: CryptoTransferResult => routers.accountProcessor forward m

      // Market Processors
      case m: DoCancelOrder => routers.marketProcessors(m.side) forward m

      // Robot Processor
      case m: DoSubmitRobot => routers.robotProcessor forward Persistent(m)
      case m: DoCancelRobot => routers.robotProcessor forward Persistent(m)
      case m: DoAddRobotDNA => routers.robotProcessor forward Persistent(m)
      case m: DoRemoveRobotDNA => routers.robotProcessor forward Persistent(m)

      // DepoistWithdraw Processor
      case m: AdminConfirmTransferFailure => routers.depositWithdrawProcessor forward m
      case m: AdminConfirmTransferSuccess => routers.depositWithdrawProcessor forward m
      case m: DoCancelTransfer => routers.depositWithdrawProcessor forward m
      case m: MultiCryptoCurrencyTransactionMessage => routers.depositWithdrawProcessor forward m
      case m: MultiTransferCryptoCurrencyResult => routers.depositWithdrawProcessor forward m

      //-------------------------------------------------------------------------
      // AccountView
      case m: QueryAccount => routers.accountView forward m
      case m: QueryRCDepositRecord => routers.accountView forward m
      case m: QueryRCWithdrawalRecord => routers.accountView forward m
      case QueryFeeConfig => routers.accountView forward QueryFeeConfig

      // MarketDepthViews
      case m: QueryMarketDepth => routers.marketDepthViews(m.side) forward m
      case m: QueryMarketDepthByPrice => routers.marketDepthViews(m.side) forward m
      case m: DoSimulateOrderSubmission => routers.marketDepthViews(m.doSubmitOrder.side) forward m

      // CandleDataView
      case m: QueryCandleData => routers.candleDataView(m.side) forward m

      // Mailer
      case m: DoSendEmail => routers.mailer forward m

      // MetricsView
      case QueryMetrics => routers.metricsView forward QueryMetrics

      // Misc Queries
      case m: QueryTransaction => routers.transactionReader forward m
      case m: QueryOrder => routers.orderReader forward m
      case m: QueryTransfer => routers.depositWithdrawReader forward m

      // ApiAuthProcessor and View
      case m: DoAddNewApiSecret => routers.apiAuthProcessor forward Persistent(m)
      case m: DoDeleteApiSecret => routers.apiAuthProcessor forward Persistent(m)
      case m: QueryApiSecrets => routers.apiAuthView forward m

      // User Asset
      case m: QueryAsset => routers.assetView forward m

      // Bitway
      case m: AllocateNewAddress => routers.bitwayProcessors(m.currency) forward m
      case m: TransferCryptoCurrency => routers.bitwayProcessors(m.currency) forward m
      case m: MultiTransferCryptoCurrency => routers.bitwayProcessors(m.currency) forward m
      case m: BitwayMessage => routers.bitwayProcessors(m.currency) forward m
      case m: AdjustAddressAmount => routers.bitwayProcessors(m.currency) forward m
      case m: QueryCryptoCurrencyAddressStatus => routers.bitwayViews(m.currency) forward m
      case m: QueryCryptoCurrencyNetworkStatus => routers.bitwayViews(m.currency) forward m
      case m: QueryReserveStatus => routers.bitwayViews(m.currency) forward m
      case m: CleanBlockChain => routers.bitwayProcessors(m.currency) forward m
      case m: SyncPrivateKeys => routers.bitwayProcessors(m.currency) forward m

      // Notification
      case m: SetNotification => routers.notification forward m
      case m: QueryNotification => routers.notification forward m

      // Monitoring
      case m: QueryActiveActors => routers.monitorService forward m

      //-------------------------------------------------------------------------
      case m =>
        log.error("Coinex received unsupported event: " + m.toString)
        sender ! MessageNotSupported(m.toString)
    }
  }
}
