package me.lsbengine.api.admin

import me.lsbengine.api.PostsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.MongoFormats._
import me.lsbengine.database.model.{MongoCollections, Post}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{UpdateWriteResult, WriteConcern}
import reactivemongo.api.{Cursor, DefaultDB}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminPostsAccessor(db: DefaultDB)
  extends DatabaseAccessor[Post](db, MongoCollections.postsCollectionName)
    with PostsAccessor {


  val trashCollection: BSONCollection = db[BSONCollection](MongoCollections.postsTrashCollectionName)

  def getPost(id: Int): Future[Option[Post]] = {
    val query = BSONDocument("id" -> id)
    super.getItem(query)
  }

  def listPosts(category: Option[String]): Future[List[Post]] = {
    val sort = BSONDocument("published" -> -1)
    category match {
      case Some(cat) =>
        val query = BSONDocument("category" -> cat)
        super.getItems(query = query, sort = sort)
      case None =>
        super.getItems(sort = sort)
    }
  }

  def updatePost(id: Int, post: Post): Future[UpdateWriteResult] = {
    val selector = BSONDocument("id" -> id)
    super.updateItem(selector, post)
  }

  def deletePost(id: Int): Future[UpdateWriteResult] = {
    val selector = BSONDocument("id" -> id)
    getCollection.findAndRemove(selector).flatMap {
      res =>
        res.result[Post] match {
          case Some(post) =>
            trashCollection.update(post, post, writeConcern = WriteConcern.Acknowledged, upsert = true)
          case None =>
            Future(UpdateWriteResult(ok = false, 0, 0, Seq(), Seq(), None, None, None))
        }
    }
  }

  def createPost(post: Post): Future[Option[Post]] = {
    val sort = BSONDocument("id" -> -1)
    super.getItems(sort = sort, maxItems = 1).flatMap {
      list =>
        val newId = list.headOption match {
          case Some(lastPost) =>
            lastPost.id + 1
          case None =>
            0
        }

        val newPost = post.copy(id = newId)
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

  def getTrash: Future[List[Post]] = {
    trashCollection.find(BSONDocument()).cursor[Post]().collect[List](maxDocs = -1, Cursor.DoneOnError[List[Post]]())
  }

  def purgeTrash: Future[Boolean] = {
    trashCollection.drop(failIfNotFound = false)
  }
}
