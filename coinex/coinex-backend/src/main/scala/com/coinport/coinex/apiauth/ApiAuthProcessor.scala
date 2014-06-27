
/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */
package com.coinport.coinex.apiauth

import akka.persistence.SnapshotOffer
import com.coinport.coinex.data._
import akka.actor._
import akka.persistence._
import com.coinport.coinex.common._
import com.coinport.coinex.util.Hash
import com.google.common.io.BaseEncoding
import com.coinport.coinex.common.PersistentId._
import ErrorCode._
import Implicits._
import akka.event.LoggingReceive

class ApiAuthProcessor(seed: String) extends ExtendedProcessor with Processor {
  override def processorId = API_AUTH_PROCESSOR <<

  val manager = new ApiAuthManager(seed)

  def receive = LoggingReceive {
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
