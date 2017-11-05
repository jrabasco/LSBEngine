package me.lsbengine.api.public

import com.github.nscala_time.time.Imports.DateTime
import me.lsbengine.api.ProjectsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Project}
import me.lsbengine.database.model.MongoFormats._
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDateTime, BSONDocument}

import scala.concurrent.Future

class PublicProjectsAccessor(db: DefaultDB)
  extends DatabaseAccessor[Project](db, MongoCollections.projectsCollectionName)
    with ProjectsAccessor {

  def getProject(id: Int): Future[Option[Project]] = {
    val now = DateTime.now
    val query = BSONDocument("id" -> id,
      "published" -> BSONDocument(
        "$lte" -> BSONDateTime(now.getMillis)
      ))
    super.getItem(query)
  }

  def listProjects: Future[List[Project]] = {
    val now = DateTime.now
    val sort = BSONDocument("published" -> -1)
    val query = BSONDocument("published" -> BSONDocument(
      "$lte" -> BSONDateTime(now.getMillis)
    ))
    super.getItems(query = query, sort = sort)
  }
}
