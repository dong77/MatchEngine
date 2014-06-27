package com.coinport.coinex.common

object PersistentId extends Enumeration {
  // Processor Ids
  val ACCOUNT_PROCESSOR = Value("p_a")
  val API_AUTH_PROCESSOR = Value("p_aa")
  val ACCOUNT_TRANSFER_PROCESSOR = Value("p_dw")
  val MARKET_PROCESSOR = Value("p_m")
  val MARKET_UPDATE_PROCESSOR = Value("p_mu")
  val ROBOT_PROCESSOR = Value("p_r")
  val USER_PROCESSOR = Value("p_u")
  val EXPORT_OPEN_DATA_PROCESSOR = Value("p_od")
  val BITWAY_PROCESSOR = Value("p_bw")

  // View Ids
  val ACCOUNT_VIEW = Value("v_a")
  val API_AUTH_VIEW = Value("v_aa")
  val CANDLE_DATA_VIEW = Value("v_cd")
  val MARKET_DEPTH_VIEW = Value("v_md")
  val METRICS_VIEW = Value("v_m")
  val ROBOT_VIEW = Value("v_r")
  val USER_VIEW = Value("v_u")
  val USER_WRITER_VIEW = Value("v_uw")
  val USER_ASSET = Value("v_ua")
  val BITWAY_VIEW = Value("v_bw")
}
