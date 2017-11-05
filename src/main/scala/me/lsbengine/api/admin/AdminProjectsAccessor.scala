package me.lsbengine.api.admin

import me.lsbengine.api.ProjectsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.MongoFormats._
import me.lsbengine.database.model.{ MongoCollections, Project}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{ UpdateWriteResult, WriteConcern }
import reactivemongo.api.{ Cursor, DefaultDB }
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminProjectsAccessor(db: DefaultDB)
  extends DatabaseAccessor[Project](db, MongoCollections.projectsCollectionName)
  with ProjectsAccessor {

  val trashCollection: BSONCollection = db[BSONCollection](MongoCollections.projectsTrashCollectionName)

  def getProject(id: Int): Future[Option[Project]] = {
    val query = BSONDocument("id" -> id)
    super.getItem(query)
  }

  def listProjects: Future[List[Project]] = {
    val sort = BSONDocument("published" -> -1)
    super.getItems(sort = sort)
  }

  def updateProject(id: Int, post: Project): Future[UpdateWriteResult] = {
    val selector = BSONDocument("id" -> id)
    super.updateItem(selector, post)
  }

  def deleteProject(id: Int): Future[UpdateWriteResult] = {
    val selector = BSONDocument("id" -> id)
    getCollection.findAndRemove(selector).flatMap {
      res =>
        res.result[Project] match {
          case Some(proj) =>
            trashCollection.update(proj, proj, writeConcern = WriteConcern.Acknowledged, upsert = true)
          case None =>
            Future(UpdateWriteResult(ok = false, 0, 0, Seq(), Seq(), None, None, None))
        }
    }
  }

  def createProject(proj: Project): Future[Option[Project]] = {
    val sort = BSONDocument("id" -> -1)
    super.getItems(sort = sort, maxItems = 1).flatMap {
      list =>
        val newId = list.headOption match {
          case Some(lastPost) =>
            lastPost.id + 1
          case None =>
            0
        }

        val newPost = proj.copy(id = newId)
        val selector = BSONDocument("id" -> newId)
        super.upsertItem(selector, newPost).map {
          res =>
            if (res.ok) {
              Some(newPost)
            } else {
              None
            }
        }
    }
  }

  def getTrash: Future[List[Project]] = {
    trashCollection.find(BSONDocument()).cursor[Project]().collect[List](maxDocs = -1, Cursor.DoneOnError[List[Project]]())
  }

  def purgeTrash: Future[Boolean] = {
    trashCollection.drop(failIfNotFound = false)
  }
}
