/**
 * Copyright (C) 2014 Coinport Inc.
 */

package com.coinport.coinex.api.model

import com.coinport.coinex.data.Currency
import com.coinport.coinex.data.UserStatus

case class User(id: Long,
  email: String,
  realName: Option[String] = None,
  password: String,
  nationalId: Option[String] = None,
  mobile: Option[String] = None,
  depositAddress: Option[Map[Currency, String]] = None,
  withdrawalAddress: Option[Map[Currency, String]] = None,
  status: UserStatus = UserStatus.Normal)
