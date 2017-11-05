package me.lsbengine.api.admin

import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Categories}
import me.lsbengine.database.model.MongoFormats.categoriesFormat
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CategoriesAccessor(database: DefaultDB)
  extends DatabaseAccessor[Categories](database, MongoCollections.categoriesCollectionName)
  with SimpleResourceAccessor[Categories] {

  override def getResource: Future[Categories] = {
    super.getItem(BSONDocument()).map(_.getOrElse(Categories(titles=List())))
  }

  override def setResource(cats: Categories): Future[UpdateWriteResult] = {
    super.upsertItem(BSONDocument(), cats)
  }
}
