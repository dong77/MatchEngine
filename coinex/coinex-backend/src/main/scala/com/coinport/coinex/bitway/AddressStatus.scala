/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package com.coinport.coinex.bitway

import scala.collection.SortedMap

import com.coinport.coinex.data._

object AddressStatus {
  def apply(s: TAddressStatus): AddressStatus = {
    AddressStatus(s.txid, s.height, SortedMap.empty[Long, List[Long]] ++ s.books.map(kv => (kv._1 -> kv._2.toList)))
  }
}

case class AddressStatus(txid: Option[String] = None, height: Option[Long] = None,
    var books: SortedMap[Long, List[Long]] = SortedMap.empty) {

  def updateTxid(txid: Option[String]): AddressStatus = {
    if (txid.isDefined)
      copy(txid = txid)
    else
      this
  }

  def updateHeight(height: Option[Long]): AddressStatus = {
    if (height.isDefined)
      copy(height = height)
    else
      this
  }

  def clearBookAfterHeight(height: Long): AddressStatus = {
    books = books.filter(kv => (kv._1 <= height))
    copy(books = books)
  }

  def updateBook(height: Option[Long], amount: Option[Long]): AddressStatus = {
    height match {
      case None => this
      case Some(h) =>
        amount match {
          case None => this
          case Some(a) =>
            var items = books.getOrElse(h, List.empty[Long])
            items = a :: items
            copy(books = (books + (h -> items)))
        }
    }
  }

  def toThrift = {
    TAddressStatus(txid, height, books.map { kv => (kv._1 -> kv._2) })
  }

  def getAddressStatusResult(currentHeight: Option[Long]) = {
    AddressStatusResult(txid, height, getAmount(currentHeight, 1))
  }

  def getAmount(currentHeight: Option[Long], confirmationNum: Int): Long = {
    if (!currentHeight.isDefined) {
      0
    } else {
      books.filter(i => i._1 <= (currentHeight.get + 1 - confirmationNum)).flatMap(kv => kv._2).sum
    }
  }
}
