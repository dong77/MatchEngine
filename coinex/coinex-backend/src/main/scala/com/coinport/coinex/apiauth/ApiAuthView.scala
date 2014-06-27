/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.apiauth

import akka.event.LoggingReceive
import com.coinport.coinex.common._
import com.coinport.coinex.data._
import com.coinport.coinex.util.MHash
import com.google.common.io.BaseEncoding
import akka.persistence.Persistent
import com.coinport.coinex.common.PersistentId._
import Implicits._

class ApiAuthView(seed: String) extends ExtendedView {
  override val processorId = API_AUTH_PROCESSOR <<
  override val viewId = API_AUTH_VIEW <<

  val manager = new ApiAuthManager(seed)

  def receive = LoggingReceive {
    case QueryApiSecrets(userId, identifier) =>
      identifier match {
        case Some(identifier) => sender ! QueryApiSecretsResult(userId, manager.getSecret(identifier))
        case None => sender ! QueryApiSecretsResult(userId, manager.getUserSecrets(userId))
      }

    case p @ Persistent(DoAddNewApiSecret(userId), _) =>
      manager.addNewSecret(userId) match {
        case Left(code) => sender ! ApiSecretOperationResult(code, manager.getUserSecrets(userId))
        case Right(_) => sender ! ApiSecretOperationResult(ErrorCode.Ok, manager.getUserSecrets(userId))
      }

    case p @ Persistent(DoDeleteApiSecret(secret), _) =>
      manager.deleteSecret(secret) match {
        case Left(code) => sender ! ApiSecretOperationResult(code, Nil)
        case Right(_) => sender ! ApiSecretOperationResult(ErrorCode.Ok, Nil)
      }
  }
}

