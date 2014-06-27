package com.coinport.coinex.common

object ConstantRole extends Enumeration {
  val user_processor = Value
  val user_mongo_writer = Value

  val account_processor = Value
  val account_view = Value

  val asset_view = Value

  val api_auth_processor = Value
  val api_auth_view = Value

  val market_update_processor = Value
  val robot_processor = Value
  val account_transfer_processor = Value

  val metrics_view = Value
  val mailer = Value

  val transaction_mongo_writer = Value
  val transaction_mongo_reader = Value

  val order_mongo_writer = Value
  val order_mongo_reader = Value

  val account_transfer_mongo_reader = Value

  val opendata_exporter = Value

  val notification_mongo = Value

  val monitor_service = Value
}

object MarketRole extends Enumeration {
  val market_processor = Value
  val market_depth_view = Value
  val candle_data_view = Value
  val market_processor_event_export = Value
}

object BitwayRole extends Enumeration {
  val bitway_processor = Value
  val bitway_receiver = Value
  val bitway_view = Value
}
