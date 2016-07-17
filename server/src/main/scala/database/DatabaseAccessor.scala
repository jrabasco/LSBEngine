package database

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DatabaseAccessor {
  val documentsCollectionName = "documents"
}

class DatabaseAccessor[T](db: DefaultDB, collectionName: String) {

  def getCollection: BSONCollection = db[BSONCollection](collectionName)

  def getItems(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    getCollection.find(query).cursor[T].collect[List]()
  }

  def getItem(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    getCollection.find(query).one[T]
  }

  def listItems(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    getItems(BSONDocument())
  }
}
