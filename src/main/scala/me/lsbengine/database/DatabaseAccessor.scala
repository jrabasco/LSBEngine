package me.lsbengine.database

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.commands.WriteConcern
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DatabaseAccessor[T](db: DefaultDB, collectionName: String) {

  def getCollection: BSONCollection = db[BSONCollection](collectionName)

  def getItems(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    getCollection.find(query).cursor[T]().collect[List]()
  }

  def getItem(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    getCollection.find(query).one[T]
  }

  def listItems(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    getItems(BSONDocument())
  }

  def upsertItem(selector: BSONDocument, item: T)(implicit writer: BSONDocumentWriter[T]): Future[UpdateWriteResult] = {
    getCollection.update(selector, item, WriteConcern.Acknowledged, upsert = true)
  }
}
