/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.service

import akka.util.Timeout
import scala.concurrent.duration._

trait AkkaService {
  implicit val timeout = Timeout(2 seconds)
  val backend = Akka.backend
}

