package com.coinport.coinex.accounts

import org.specs2.mutable.Specification
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data._
import ErrorCode._

class AccountManagerSpec extends Specification {
  "account manager data struct" should {
    "update abCodeMap and can get them all" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.abCodeMap.get(0).get.amount mustEqual 1000
      manager.abCodeMap.get(0).get.id mustEqual 0
      manager.abCodeMap.get(0).get.dUserId mustEqual None
      manager.abCodeMap.get(0).get.wUserId mustEqual 1001
      manager.abCodeMap.get(0).get.codeA mustEqual "32DA834CE87FC390"
      manager.abCodeMap.get(0).get.codeB mustEqual "DA8332DA8348332DACE88332DA7FC390"
      manager.codeAIndexMap("32DA834CE87FC390") mustEqual 0
      manager.codeBIndexMap("DA8332DA8348332DACE88332DA7FC390") mustEqual 0
      (manager.abCodeMap.get(0).get.created.get / 1000 + 365 * 24 * 3600)
      manager.createABCodeTransaction(1001, "2222222222222", "33333333333333333", 1000000)
      manager.abCodeMap.get(1).get.amount mustEqual 1000000
      manager.codeAIndexMap("2222222222222") mustEqual 1
      manager.codeBIndexMap("33333333333333333") mustEqual 1
    }
  }

  "generate ab code" should {
    "a b code generate right" in {
      val manager = new AccountManager()
      val (a, b) = manager.generateABCode()
      a.matches("[A-F0-9]{16}") mustEqual true
      b.matches("[A-F0-9]{32}") mustEqual true
    }
  }

  "isCodeAAvailable & freezeABCode" should {
    "a code's status is right" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.isCodeAAvailable(1002, "32DA834CE87FC390") mustEqual true
      manager.abCodeMap(0).dUserId.isEmpty mustEqual true
      manager.abCodeMap(0).queryExpireTime.isEmpty mustEqual true
      manager.freezeABCode(1002, "32DA834CE87FC390")
      manager.abCodeMap(0).dUserId.get mustEqual 1002
      manager.abCodeMap(0).queryExpireTime.isDefined mustEqual true
      manager.isCodeAAvailable(1002, "32DA834CE87FC390") mustEqual true
      manager.isCodeAAvailable(1003, "32DA834CE87FC390") mustEqual false
      manager.isCodeAAvailable(1002, "32DA834CE87FC3901") mustEqual false
      manager.abCodeMap += 0L -> manager.abCodeMap(0).copy(queryExpireTime = Some(System.currentTimeMillis / 1000 - 3601))
      manager.isCodeAAvailable(1003, "32DA834CE87FC390") mustEqual true
      manager.createABCodeTransaction(1004, "22DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 100021)
      manager.abCodeMap += 1L -> manager.abCodeMap(1).copy(status = RechargeCodeStatus.Unused)
      manager.isCodeAAvailable(1004, "22DA834CE87FC390") mustEqual true
      manager.abCodeMap += 1L -> manager.abCodeMap(1).copy(status = RechargeCodeStatus.Frozen)
      manager.isCodeAAvailable(1004, "22DA834CE87FC390") mustEqual false
      manager.abCodeMap += 1L -> manager.abCodeMap(1).copy(status = RechargeCodeStatus.Confirming)
      manager.isCodeAAvailable(1004, "22DA834CE87FC390") mustEqual false
      manager.abCodeMap += 1L -> manager.abCodeMap(1).copy(status = RechargeCodeStatus.RechargeDone)
      manager.isCodeAAvailable(1004, "22DA834CE87FC390") mustEqual false
    }
  }

  "verifyCodeB" should {
    "every situation must be handled correctly" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual true
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC391")._1 mustEqual false
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC391")._2 mustEqual InvalidBCode
      manager.abCodeMap += 0L -> manager.abCodeMap(0).copy(dUserId = Some(1002))
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual true
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual true
      manager.abCodeMap += 0L -> manager.abCodeMap(0).copy(status = RechargeCodeStatus.Frozen)
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual true
      manager.abCodeMap += 0L -> manager.abCodeMap(0).copy(status = RechargeCodeStatus.Confirming)
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual false
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._2 mustEqual UsedBCode
      manager.abCodeMap += 0L -> manager.abCodeMap(0).copy(status = RechargeCodeStatus.RechargeDone)
      manager.isCodeBAvailable(1002, "DA8332DA8348332DACE88332DA7FC390")._1 mustEqual false
      manager.createABCodeTransaction(1001, "1111111111111111", "22222222222222222222222222222222", 1000)
      manager.freezeABCode(1003, "1111111111111111")
      manager.isCodeBAvailable(1002, "22222222222222222222222222222222")._1 mustEqual false
      manager.isCodeBAvailable(1003, "22222222222222222222222222222222")._1 mustEqual true
    }
  }

  "bCodeRecharge" should {
    "recharge" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.freezeABCode(1002, "32DA834CE87FC390")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC390")
      manager.abCodeMap(0).status mustEqual RechargeCodeStatus.Confirming
    }
  }

  "confirmRecharge" should {
    "confirm recharge" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.freezeABCode(1002, "32DA834CE87FC390")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC390")
      manager.confirmRecharge(1001, "DA8332DA8348332DACE88332DA7FC390")
      manager.abCodeMap(0).status mustEqual RechargeCodeStatus.RechargeDone
    }
  }

  "getRCDepositRecords" should {
    "get records" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.getRCDepositRecords(1002).isEmpty mustEqual true
      manager.freezeABCode(1002, "32DA834CE87FC390")
      manager.getRCDepositRecords(1002).size mustEqual 1
      manager.getRCDepositRecords(1002)(0).status mustEqual RechargeCodeStatus.Frozen
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC390")
      manager.getRCDepositRecords(1002).size mustEqual 1
      manager.getRCDepositRecords(1002)(0).status mustEqual RechargeCodeStatus.Confirming
      manager.createABCodeTransaction(1003, "32DA834CE87FC3901", "DA8332DA8348332DACE88332DA7FC3901", 2000)
      manager.getRCDepositRecords(1002).size mustEqual 1
      manager.getRCDepositRecords(1002)(0).status mustEqual RechargeCodeStatus.Confirming
      manager.freezeABCode(1002, "32DA834CE87FC3901")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC3901")
      manager.getRCDepositRecords(1002).size mustEqual 2
      manager.createABCodeTransaction(1001, "32DA834CE87FC3902", "DA8332DA8348332DACE88332DA7FC3902", 2000)
      manager.getRCDepositRecords(1002)(1).status mustEqual RechargeCodeStatus.Confirming
      manager.freezeABCode(1002, "32DA834CE87FC3902")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC3902")
      manager.confirmRecharge(1002, "DA8332DA8348332DACE88332DA7FC3902")
      manager.getRCDepositRecords(1002).size mustEqual 3
      manager.getRCDepositRecords(1002)(0).status mustEqual RechargeCodeStatus.RechargeDone
      manager.createABCodeTransaction(1001, "32DA834CE87FC3903", "DA8332DA8348332DACE88332DA7FC3903", 2000)
      manager.freezeABCode(1004, "32DA834CE87FC3903")
      manager.bCodeRecharge(1004, "DA8332DA8348332DACE88332DA7FC3903")
      manager.getRCDepositRecords(1002).size mustEqual 3
      manager.getRCDepositRecords(1004).size mustEqual 1
    }
  }

  "getRCWithdrawalRecords" should {
    "get RcWithdrawal records" in {
      val manager = new AccountManager()
      manager.createABCodeTransaction(1001, "32DA834CE87FC390", "DA8332DA8348332DACE88332DA7FC390", 1000)
      manager.getRCDepositRecords(1002).isEmpty mustEqual true
      manager.freezeABCode(1002, "32DA834CE87FC390")
      manager.getRCWithdrawalRecords(1001).size mustEqual 1
      manager.getRCWithdrawalRecords(1001)(0).status mustEqual RechargeCodeStatus.Frozen
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC390")
      manager.getRCWithdrawalRecords(1001).size mustEqual 1
      manager.getRCWithdrawalRecords(1001)(0).status mustEqual RechargeCodeStatus.Confirming
      manager.createABCodeTransaction(1003, "32DA834CE87FC3901", "DA8332DA8348332DACE88332DA7FC3901", 2000)
      manager.getRCWithdrawalRecords(1001).size mustEqual 1
      manager.freezeABCode(1002, "32DA834CE87FC3901")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC3901")
      manager.createABCodeTransaction(1001, "32DA834CE87FC3902", "DA8332DA8348332DACE88332DA7FC3902", 2000)
      manager.freezeABCode(1002, "32DA834CE87FC3902")
      manager.bCodeRecharge(1002, "DA8332DA8348332DACE88332DA7FC3902")
      manager.confirmRecharge(1002, "DA8332DA8348332DACE88332DA7FC3902")
      manager.getRCWithdrawalRecords(1001).size mustEqual 2
      manager.getRCWithdrawalRecords(1001)(0).status mustEqual RechargeCodeStatus.RechargeDone
      manager.createABCodeTransaction(1001, "32DA834CE87FC3903", "DA8332DA8348332DACE88332DA7FC3903", 2000)
      manager.freezeABCode(1004, "32DA834CE87FC3903")
      manager.bCodeRecharge(1004, "DA8332DA8348332DACE88332DA7FC3903")
      manager.getRCWithdrawalRecords(1001).size mustEqual 3
      manager.getRCWithdrawalRecords(1003).size mustEqual 1
    }
  }

  "hot cold transfer test" should {
    "get corrent transfer amount as well as direction" in {
      val manager = new AccountManager(0L, Map(Btc -> HotColdTransferStrategy(0.5, 0.3)))
      manager.needHotColdTransfer(Btc) mustEqual None
      manager.updateHotCashAccount(CashAccount(Btc, 10, 0, 0))
      manager.needHotColdTransfer(Btc) mustEqual Some(6)
      manager.updateHotCashAccount(CashAccount(Btc, -10, 0, 0))
      manager.needHotColdTransfer(Btc) mustEqual None
      manager.updateColdCashAccount(CashAccount(Btc, 10, 0, 0))
      manager.needHotColdTransfer(Btc) mustEqual Some(-4)
      manager.updateColdCashAccount(CashAccount(Btc, -4, 0, 0))
      manager.updateHotCashAccount(CashAccount(Btc, 4, 0, 0))
      manager.updateHotCashAccount(CashAccount(Btc, 10, 0, 0))
      manager.needHotColdTransfer(Btc) mustEqual Some(6)
      manager.updateColdCashAccount(CashAccount(Btc, 6, 0, 0))
      manager.updateHotCashAccount(CashAccount(Btc, -6, 0, 0))
      manager.updateColdCashAccount(CashAccount(Btc, 100, 0, 0))
      manager.needHotColdTransfer(Btc) mustEqual Some(-40)
    }
  }
}
