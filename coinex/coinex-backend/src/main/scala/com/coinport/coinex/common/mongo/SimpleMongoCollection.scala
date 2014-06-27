package com.coinport.coinex.common.mongo

import com.mongodb.casbah.Imports._
import com.coinport.coinex.serializers._
import com.mongodb.util.JSON
import com.coinport.coinex.serializers.ThriftEnumJson4sSerialization
import org.json4s.native.Serialization.{ read, write }

sealed trait SimpleMongoCollection[T <: AnyRef] {
  val coll: MongoCollection
  val DATA = "data"
  val ID = "_id"

  def extractId(obj: T): Long
  def get(id: Long): Option[T]
  def put(data: T): Unit
}

abstract class SimpleJsonMongoCollection[T <: AnyRef, S <: T](implicit man: Manifest[S]) extends SimpleMongoCollection[T] {
  import org.json4s.native.Serialization.{ read, write }
  implicit val formats = ThriftEnumJson4sSerialization.formats
  def get(id: Long) = coll.findOne(MongoDBObject(ID -> id)) map { json => read[S](json.get(DATA).toString) }
  def put(data: T) = coll += MongoDBObject(ID -> extractId(data), DATA -> JSON.parse(write(data)))

  def find(q: MongoDBObject, skip: Int, limit: Int): Seq[T] =
    coll.find(q).sort(MongoDBObject(ID -> -1)).skip(skip).limit(limit).map { json => read[S](json.get(DATA).toString) }.toSeq

  def count(q: MongoDBObject): Long = coll.count(q)
}

abstract class SimpleBinaryMongoCollection[T <: AnyRef, S <: T](implicit man: Manifest[S]) extends SimpleMongoCollection[T] {
  val serializer = new ThriftBinarySerializer
  def get(id: Long) = coll.findOne(MongoDBObject(ID -> id)) map { json =>
    serializer.fromBinary(json.get(DATA).asInstanceOf[Array[Byte]], Some(man.runtimeClass)).asInstanceOf[T]
  }
  def put(data: T) = coll += MongoDBObject(ID -> extractId(data), DATA -> serializer.toBinary(data))
}