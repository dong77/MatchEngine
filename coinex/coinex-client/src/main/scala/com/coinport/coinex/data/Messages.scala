/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 * All classes here are case-classes or case-objects. This is required since we are
 * maintaining an in-memory state that's immutable, so that while snapshot is taken,
 * the in-memory state can still be updated.
 */

package com.coinport.coinex.data

// TODO(d): put all these classes into thrift file.
case object QueryMetrics

case object QueryActorStats

case object ActivateRobotsNow

case class DoSubmitRobot(robot: Robot)

case class DoCancelRobot(id: Long)

case object ListenAtRedis

case object DoExportData

case object TryFetchAddresses

case object TrySyncHotAddresses

case class FetchAddresses(currency: Currency)

case object QueryRecoverStats

case object QueryFeeConfig
