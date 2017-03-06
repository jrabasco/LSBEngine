package me.lsbengine.api.admin

import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, NavBarConf}
import me.lsbengine.database.model.MongoFormats.navBarFormat
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NavBarConfAccessor(database: DefaultDB)
  extends DatabaseAccessor[NavBarConf](database, MongoCollections.navBarConfCollectionName)
  with SimpleResourceAccessor[NavBarConf] {

  override def getResource: Future[NavBarConf] = {
    super.getItem(BSONDocument()).map(_.getOrElse(NavBarConf(projects = true, about = true)))
  }

  override def setResource(navBarConf: NavBarConf): Future[UpdateWriteResult] = {
    super.upsertItem(BSONDocument(), navBarConf)
  }
}
