package me.lsbengine.api.admin

import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{AboutMe, MongoCollections}
import me.lsbengine.database.model.MongoFormats.aboutMeFormat
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AboutMeAccessor(database: DefaultDB)
  extends DatabaseAccessor[AboutMe](database, MongoCollections.aboutMeCollectionName)
  with SimpleResourceAccessor[AboutMe] {

  override def getResource: Future[AboutMe] = {
    super.getItem(BSONDocument()).map(_.getOrElse(AboutMe(introduction = None, resume = None)))
  }

  override def setResource(aboutMe: AboutMe): Future[UpdateWriteResult] = {
    super.upsertItem(BSONDocument(), aboutMe)
  }
}
