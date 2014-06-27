/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.bitway

import org.specs2.mutable._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Set

import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import Implicits._
import Currency._

class BitwayManagerSpec extends Specification {
  import CryptoCurrencyAddressType._
  import TransferType._
  import TransferStatus._

  private def getBtcInternalAmount(amount: Double): Option[Long] = Some(new CurrencyWrapper(amount).internalValue(Btc))

  private def getBtcExternalAmount(amount: Long): Option[Double] = Some(new CurrencyWrapper(amount).externalValue(Btc))

  "BitwayManager" should {
    "address accocate test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)

      bwm.getSupportedCurrency mustEqual Btc

      bwm.isDryUp mustEqual true
      bwm.faucetAddress(Unused, Set(CryptoAddress("d1"), CryptoAddress("d2"), CryptoAddress("d3"), CryptoAddress("d4"), CryptoAddress("d5"), CryptoAddress("d6")))
      bwm.isDryUp mustEqual false

      val d1 = bwm.allocateAddress
      val d1_ = bwm.allocateAddress
      d1 mustEqual d1_
      d1._2 mustEqual false

      bwm.addressAllocated(1, d1._1.get)
      val d2 = bwm.allocateAddress
      d1 mustNotEqual d2
      d2._2 mustEqual false
      bwm.addressAllocated(1, d2._1.get)
      val d3 = bwm.allocateAddress
      d3._2 mustEqual false
      bwm.addressAllocated(1, d3._1.get)
      val d4 = bwm.allocateAddress
      d4._2 mustEqual true
      bwm.addressAllocated(1, d4._1.get)
      val d5 = bwm.allocateAddress
      d5._1 mustNotEqual None
      d5._2 mustEqual true
      bwm.addressAllocated(1, d5._1.get)
      val d6 = bwm.allocateAddress
      d6._1 mustNotEqual None
      d6._2 mustEqual true
      bwm.addressAllocated(1, d6._1.get)
      val none = bwm.allocateAddress
      none mustEqual (None, true)
      val noneAgain = bwm.allocateAddress
      noneAgain mustEqual (None, true)
    }

    "get tx type test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(User, Set(CryptoAddress("u1"), CryptoAddress("u2"), CryptoAddress("u3"), CryptoAddress("u4"), CryptoAddress("u5"), CryptoAddress("u6")))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1"), CryptoAddress("h2")))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1")))

      bwm.getTransferType(Set("d1"), Set("d2")) mustEqual None
      bwm.getTransferType(Set("u1", "d1"), Set("h1", "u1")) mustEqual Some(UserToHot)
      bwm.getTransferType(Set("u1", "d1"), Set("h1")) mustEqual Some(UserToHot)
      bwm.getTransferType(Set("h1"), Set("c1", "h1")) mustEqual Some(HotToCold)
      bwm.getTransferType(Set("c1"), Set("h1", "c1")) mustEqual Some(ColdToHot)
      bwm.getTransferType(Set("d1"), Set("u1", "d1")) mustEqual Some(Deposit)
      bwm.getTransferType(Set("h1"), Set("d1", "h1")) mustEqual Some(Withdrawal)
      bwm.getTransferType(Set("u1"), Set("d1")) mustEqual Some(Unknown)
    }

    "block chain test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.getBlockIndexes mustEqual Some(ArrayBuffer.empty[BlockIndex])
      bwm.getCurrentBlockIndex mustEqual None
      bwm.appendBlockChain(List(
        BlockIndex(Some("b1"), Some(1)),
        BlockIndex(Some("b2"), Some(2)),
        BlockIndex(Some("b3"), Some(3)),
        BlockIndex(Some("b4"), Some(4)),
        BlockIndex(Some("b5"), Some(5)),
        BlockIndex(Some("b6"), Some(6))
      ), None)
      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(None,
        CryptoCurrencyBlock(BlockIndex(Some("b1"), Some(1)), BlockIndex(None, None), Nil)
      )) mustEqual BlockContinuityEnum.DUP

      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(None,
        CryptoCurrencyBlock(BlockIndex(Some("b7"), Some(7)), BlockIndex(Some("b6"), Some(6)), Nil)
      )) mustEqual BlockContinuityEnum.SUCCESSOR

      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(None,
        CryptoCurrencyBlock(BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b7"), Some(7)), Nil)
      )) mustEqual BlockContinuityEnum.GAP

      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(Some(BlockIndex(Some("b6"), Some(6))),
        CryptoCurrencyBlock(BlockIndex(Some("b7"), Some(7)), BlockIndex(Some("b6"), Some(6)), Nil)
      )) mustEqual BlockContinuityEnum.SUCCESSOR

      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(Some(BlockIndex(Some("b2"), Some(2))),
        CryptoCurrencyBlock(BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b7p"), Some(7)), Nil)
      )) mustEqual BlockContinuityEnum.REORG

      bwm.getBlockContinuity(CryptoCurrencyBlockMessage(Some(BlockIndex(None, None)),
        CryptoCurrencyBlock(BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b7"), Some(7)), Nil)
      )) mustEqual BlockContinuityEnum.OTHER_BRANCH

      bwm.appendBlockChain(List(
        BlockIndex(Some("b7"), Some(7)),
        BlockIndex(Some("b8"), Some(8))
      ), None)

      bwm.getBlockIndexes mustEqual Some(ArrayBuffer(BlockIndex(Some("b1"), Some(1)), BlockIndex(Some("b2"), Some(2)), BlockIndex(Some("b3"), Some(3)), BlockIndex(Some("b4"), Some(4)), BlockIndex(Some("b5"), Some(5)), BlockIndex(Some("b6"), Some(6)), BlockIndex(Some("b7"), Some(7)), BlockIndex(Some("b8"), Some(8))))

      bwm.appendBlockChain(List(
        BlockIndex(Some("b8"), Some(8)),
        BlockIndex(Some("b9"), Some(9))
      ), None)
      bwm.getBlockIndexes mustEqual Some(ArrayBuffer(BlockIndex(Some("b1"), Some(1)), BlockIndex(Some("b2"), Some(2)), BlockIndex(Some("b3"), Some(3)), BlockIndex(Some("b4"), Some(4)), BlockIndex(Some("b5"), Some(5)), BlockIndex(Some("b6"), Some(6)), BlockIndex(Some("b7"), Some(7)), BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b9"), Some(9))))

      bwm.appendBlockChain(List(
        BlockIndex(Some("b10"), Some(10)),
        BlockIndex(Some("b11"), Some(11))
      ), Some(BlockIndex(Some("b9"), Some(9))))

      bwm.getBlockIndexes mustEqual Some(ArrayBuffer(BlockIndex(Some("b3"), Some(3)), BlockIndex(Some("b4"), Some(4)), BlockIndex(Some("b5"), Some(5)), BlockIndex(Some("b6"), Some(6)), BlockIndex(Some("b7"), Some(7)), BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b8"), Some(8)), BlockIndex(Some("b9"), Some(9)), BlockIndex(Some("b10"), Some(10)), BlockIndex(Some("b11"), Some(11))))

      bwm.appendBlockChain(List(
        BlockIndex(Some("b10"), Some(10)),
        BlockIndex(Some("b11"), Some(11))
      ), Some(BlockIndex(Some("b3"), Some(3))))

      bwm.getBlockIndexes mustEqual Some(ArrayBuffer(BlockIndex(Some("b3"), Some(3)), BlockIndex(Some("b10"), Some(10)), BlockIndex(Some("b11"), Some(11))))

      bwm.appendBlockChain(List(
        BlockIndex(Some("b10"), Some(10)),
        BlockIndex(Some("b11"), Some(11))
      ), Some(BlockIndex(None, None)))
      bwm.getBlockIndexes mustEqual Some(ArrayBuffer(BlockIndex(Some("b3"), Some(3)), BlockIndex(Some("b10"), Some(10)), BlockIndex(Some("b11"), Some(11)), BlockIndex(Some("b10"), Some(10)), BlockIndex(Some("b11"), Some(11))))
    }

    "tx generation test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(Unused, Set(CryptoAddress("u7")))
      bwm.faucetAddress(User, Set(CryptoAddress("u1"), CryptoAddress("u2"), CryptoAddress("u3"), CryptoAddress("u4"), CryptoAddress("u5"), CryptoAddress("u6")))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1"), CryptoAddress("h2"), CryptoAddress("h3")))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1")))

      bwm.addressAllocated(1, "u7")

      val bi1 = BlockIndex(Some("b1"), Some(1))
      val rawTx = CryptoCurrencyTransaction(
        txid = Some("t1"),
        inputs = Some(List(CryptoCurrencyTransactionPort("u7", Some(1.1)))),
        outputs = Some(List(CryptoCurrencyTransactionPort("h1", Some(0.9)))),
        includedBlock = Some(bi1), status = Confirming)
      bwm.completeCryptoCurrencyTransaction(rawTx, None, None) mustEqual Some(CryptoCurrencyTransaction(None, Some("t1"), None, Some(List(CryptoCurrencyTransactionPort("u7", Some(1.1), getBtcInternalAmount(1.1), Some(1)))), Some(List(CryptoCurrencyTransactionPort("h1", Some(0.9), getBtcInternalAmount(0.9), Some(-1)))), None, None, Some(UserToHot), Confirming, minerFee = getBtcInternalAmount(0.2)))

      val infos = Seq(
        CryptoCurrencyTransferInfo(1, Some("i1"), Some(1000)),
        CryptoCurrencyTransferInfo(2, Some("i2"), Some(80)))
      bwm.completeTransferInfos(infos) mustEqual (List(CryptoCurrencyTransferInfo(1, Some("i1"), Some(1000), getBtcExternalAmount(1000), None), CryptoCurrencyTransferInfo(2, Some("i2"), Some(80), getBtcExternalAmount(80), None)), false)

      val tx1 = CryptoCurrencyTransaction(
        txid = Some("t1"),
        inputs = Some(List(CryptoCurrencyTransactionPort("h1", Some(1.1)))),
        outputs = Some(List(CryptoCurrencyTransactionPort("d1", Some(0.9)))),
        includedBlock = Some(bi1), status = Confirming)
      val tx2 = CryptoCurrencyTransaction(
        txid = Some("t2"),
        inputs = Some(List(CryptoCurrencyTransactionPort("h2", Some(2.1)))),
        outputs = Some(List(CryptoCurrencyTransactionPort("d2", Some(2.9)))),
        includedBlock = Some(bi1), status = Confirming)
      val tx3 = CryptoCurrencyTransaction(
        txid = Some("t3"),
        inputs = Some(List(
          CryptoCurrencyTransactionPort("h3", Some(3.1)),
          CryptoCurrencyTransactionPort("h4", Some(2.6)))),
        outputs = Some(List(
          CryptoCurrencyTransactionPort("d3", Some(3.9)),
          CryptoCurrencyTransactionPort("d4", Some(0.9))
        )), includedBlock = Some(bi1), status = Confirming)
      val tx4 = CryptoCurrencyTransaction(
        txid = Some("t4"),
        inputs = Some(List(CryptoCurrencyTransactionPort("h4", Some(4.1)))),
        outputs = Some(List(CryptoCurrencyTransactionPort("d4", Some(4.9)))),
        includedBlock = Some(bi1), status = Confirming)

      val block = CryptoCurrencyBlock(
        BlockIndex(Some("b10"), Some(10)), BlockIndex(Some("b9"), Some(9)), List(tx1, tx2, tx3, tx4))

      bwm.extractTxsFromBlock(block) mustEqual List(
        CryptoCurrencyTransaction(None, Some("t1"), None,
          Some(List(CryptoCurrencyTransactionPort("h1", Some(1.1), getBtcInternalAmount(1.1), Some(-1)))),
          Some(List(CryptoCurrencyTransactionPort("d1", Some(0.9), getBtcInternalAmount(0.9)))),
          Some(BlockIndex(Some("b9"), Some(9))),
          Some(BlockIndex(Some("b10"), Some(10))), Some(Withdrawal), Confirming, minerFee = getBtcInternalAmount(0.2)),
        CryptoCurrencyTransaction(None, Some("t2"), None,
          Some(List(CryptoCurrencyTransactionPort("h2", Some(2.1), getBtcInternalAmount(2.1), Some(-1)))),
          Some(List(CryptoCurrencyTransactionPort("d2", Some(2.9), getBtcInternalAmount(2.9)))),
          Some(BlockIndex(Some("b9"), Some(9))),
          Some(BlockIndex(Some("b10"), Some(10))), Some(Withdrawal), Confirming), // error case
        CryptoCurrencyTransaction(None, Some("t3"), None,
          Some(List(
            CryptoCurrencyTransactionPort("h3", Some(3.1), getBtcInternalAmount(3.1), Some(-1)),
            CryptoCurrencyTransactionPort("h4", Some(2.6), getBtcInternalAmount(2.6))
          )),
          Some(List(
            CryptoCurrencyTransactionPort("d3", Some(3.9), getBtcInternalAmount(3.9)),
            CryptoCurrencyTransactionPort("d4", Some(0.9), getBtcInternalAmount(0.9))
          )),
          Some(BlockIndex(Some("b9"), Some(9))), Some(BlockIndex(Some("b10"), Some(10))),
          Some(Withdrawal), Confirming, minerFee = getBtcInternalAmount(0.9)))
    }

    "getAddressStatus/getNetworkStatus/adjustAddressAmount/getReserveAmounts test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(User, Set(CryptoAddress("u1"), CryptoAddress("u2"), CryptoAddress("u3"), CryptoAddress("u4"), CryptoAddress("u5"), CryptoAddress("u6")))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1"), CryptoAddress("h2")))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1")))

      val bi1 = BlockIndex(Some("b1"), Some(1))
      val bi2 = BlockIndex(Some("b2"), Some(2))

      bwm.updateAddressStatus(Seq(
        CryptoCurrencyTransaction(
          txid = Some("t1"),
          inputs = Some(List(CryptoCurrencyTransactionPort("h1", Some(1.2)))),
          outputs = Some(List(CryptoCurrencyTransactionPort("u1", Some(1.0)),
            CryptoCurrencyTransactionPort("h1", Some(0.2))
          )),
          includedBlock = Some(bi1),
          status = Confirming
        )), bi1.height)
      bwm.updateAddressStatus(Seq(
        CryptoCurrencyTransaction(
          txid = Some("t2"),
          inputs = Some(List(CryptoCurrencyTransactionPort("h1", Some(0.2)), CryptoCurrencyTransactionPort("h2", Some(0.4)))),
          outputs = Some(List(CryptoCurrencyTransactionPort("u2", Some(0.2)), CryptoCurrencyTransactionPort("h1", Some(0.1)), CryptoCurrencyTransactionPort("h2", Some(0.1)))),
          includedBlock = Some(bi2),
          status = Confirming
        )), bi2.height)
      bwm.appendBlockChain(List(BlockIndex(Some("b1"), Some(1)), BlockIndex(Some("b2"), Some(2)),
        BlockIndex(Some("b3"), Some(3))))

      bwm.updateLastAlive(1234L)
      bwm.getNetworkStatus.copy(queryTimestamp = None) mustEqual CryptoCurrencyNetworkStatus(Some("b3"), Some(3L), Some(1234L), None)

      bwm.getAddressStatus(Hot) mustEqual Map("h2" -> AddressStatusResult(Some("t2"), Some(2), getBtcInternalAmount(-0.3).get), "h1" -> AddressStatusResult(Some("t2"), Some(2), getBtcInternalAmount(-1.1).get))
      bwm.getAddressStatus(User) mustEqual Map("u2" -> AddressStatusResult(Some("t2"), Some(2), getBtcInternalAmount(0.2).get), "u1" -> AddressStatusResult(Some("t1"), Some(1), getBtcInternalAmount(1).get))
      bwm.getAddressStatus(Cold) mustEqual Map.empty[String, BlockIndex]
      bwm.getReserveAmounts mustEqual Map(Hot -> getBtcInternalAmount(-1.4).get, Cold -> 0, User -> getBtcInternalAmount(1.2).get)

      bwm.updateBlock(Some(BlockIndex(Some("b1"), Some(1))), CryptoCurrencyBlock(
        index = BlockIndex(Some("b2"), Some(2)),
        prevIndex = BlockIndex(Some("b1"), Some(1)),
        txs = List(
          CryptoCurrencyTransaction(
            txid = Some("t0"),
            inputs = Some(List(CryptoCurrencyTransactionPort("coinbase", Some(0)))),
            outputs = Some(List(CryptoCurrencyTransactionPort("u1", Some(25.2)))),
            status = Confirming
          ),
          CryptoCurrencyTransaction(
            txid = Some("t2"),
            inputs = Some(List(CryptoCurrencyTransactionPort("h1", Some(0.4)), CryptoCurrencyTransactionPort("h1", Some(0.2)))),
            outputs = Some(List(CryptoCurrencyTransactionPort("u2", Some(0.2)), CryptoCurrencyTransactionPort("u2", Some(0.2)))),
            status = Confirming
          )))
      )

      bwm.getAddressStatus(Hot) mustEqual Map("h2" -> AddressStatusResult(Some("t2"), Some(2), 0), "h1" -> AddressStatusResult(Some("t2"), Some(2), getBtcInternalAmount(-1.6).get))
      bwm.getAddressStatus(User) mustEqual Map("u2" -> AddressStatusResult(Some("t2"), Some(2), getBtcInternalAmount(0.4).get), "u1" -> AddressStatusResult(Some("t0"), Some(2), getBtcInternalAmount(26.2).get))
      bwm.getAddressStatus(Cold) mustEqual Map.empty[String, BlockIndex]
      bwm.getReserveAmounts mustEqual Map(Hot -> getBtcInternalAmount(-1.6).get, Cold -> 0, User -> getBtcInternalAmount(26.6).get)

      bwm.getAddressAmount("h1") mustEqual getBtcInternalAmount(-1.6).get
      bwm.canAdjustAddressAmount("h1", getBtcInternalAmount(1.5).get) mustEqual false
      bwm.canAdjustAddressAmount("h1", getBtcInternalAmount(1.7).get) mustEqual true
      bwm.adjustAddressAmount("h1", getBtcInternalAmount(1.7).get)
      bwm.getAddressAmount("h1") mustEqual getBtcInternalAmount(0.1).get
    }

    "disable withdrawal to deposit address" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(User, Set(CryptoAddress("u1"), CryptoAddress("u2"), CryptoAddress("u3"), CryptoAddress("u4"), CryptoAddress("u5"), CryptoAddress("u6")))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1"), CryptoAddress("h2")))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1")))

      bwm.includeWithdrawalToDepositAddress(Seq(
        CryptoCurrencyTransferInfo(1, from = Some("h1"), to = Some("c1")),
        CryptoCurrencyTransferInfo(2, from = Some("c1"), to = Some("h2"))
      )) mustEqual false

      bwm.includeWithdrawalToDepositAddress(Seq(
        CryptoCurrencyTransferInfo(2, from = Some("c1"), to = Some("h2")),
        CryptoCurrencyTransferInfo(1, from = Some("h1"), to = Some("u1"))
      )) mustEqual true

      bwm.privateKeysBackup mustEqual Map.empty[String, String]
    }

    "faucetAddress with private key" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(User, Set(CryptoAddress("u1", Some("p1")), CryptoAddress("u2", Some("p2")), CryptoAddress("u3", Some("p3")), CryptoAddress("u4", Some("p4")), CryptoAddress("u5", Some("p5")), CryptoAddress("u6", Some("p6"))))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1", Some("ph1")), CryptoAddress("h2", Some("ph2"))))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1", Some("pc1"))))

      bwm.includeWithdrawalToDepositAddress(Seq(
        CryptoCurrencyTransferInfo(1, from = Some("h1"), to = Some("c1")),
        CryptoCurrencyTransferInfo(2, from = Some("c1"), to = Some("h2"))
      )) mustEqual false

      bwm.includeWithdrawalToDepositAddress(Seq(
        CryptoCurrencyTransferInfo(2, from = Some("c1"), to = Some("h2")),
        CryptoCurrencyTransferInfo(1, from = Some("h1"), to = Some("u1"))
      )) mustEqual true

      bwm.privateKeysBackup mustEqual Map(
        "u1" -> "p1",
        "u2" -> "p2",
        "u3" -> "p3",
        "u4" -> "p4",
        "u5" -> "p5",
        "u6" -> "p6",
        "h1" -> "ph1",
        "h2" -> "ph2",
        "c1" -> "pc1")

      bwm.getPubKeys() mustEqual Set("u1", "u2", "u3", "u4", "u5", "u6", "h1", "h2", "c1")
      bwm.syncPrivateKeys(List(
        CryptoAddress("u1", Some("np1")),
        CryptoAddress("u2", None),
        CryptoAddress("h2", Some("nph2")),
        CryptoAddress("u3", Some("p3"))))
      bwm.getPubKeys() mustEqual Set("u1", "u2", "u3", "h2")
      bwm.privateKeysBackup mustEqual Map(
        "u1" -> "np1",
        "u2" -> "no-priv-key",
        "u3" -> "p3",
        "h2" -> "nph2")
    }

    "syncHotAddresses test" in {
      val bwm = new BitwayManager(Btc, 10, Nil)
      bwm.faucetAddress(User, Set(CryptoAddress("u1", Some("p1")), CryptoAddress("u2", Some("p2")), CryptoAddress("u3", Some("p3")), CryptoAddress("u4", Some("p4")), CryptoAddress("u5", Some("p5")), CryptoAddress("u6", Some("p6"))))
      bwm.faucetAddress(Hot, Set(CryptoAddress("h1", Some("ph1")), CryptoAddress("h2", Some("ph2"))))
      bwm.faucetAddress(Cold, Set(CryptoAddress("c1", Some("pc1"))))

      bwm.syncHotAddresses(Set(CryptoAddress("h1"), CryptoAddress("h3", Some("ph3")), CryptoAddress("h2", Some("other")), CryptoAddress("h4")))

      bwm.privateKeysBackup mustEqual Map(
        "u1" -> "p1",
        "u2" -> "p2",
        "u3" -> "p3",
        "u4" -> "p4",
        "u5" -> "p5",
        "u6" -> "p6",
        "h1" -> "ph1",
        "h2" -> "ph2",
        "h3" -> "ph3",
        "c1" -> "pc1")
    }
  }
}
