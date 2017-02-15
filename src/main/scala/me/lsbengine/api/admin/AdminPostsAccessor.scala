package me.lsbengine.api.admin

import com.github.nscala_time.time.Imports._
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


  val trashCollection: BSONCollection = db[BSONCollection](MongoCollections.trashCollectionName)

  def getPost(id: Int): Future[Option[Post]] = {
    val query = BSONDocument("id" -> id)
    super.getItem(query)
  }

  def listPosts: Future[List[Post]] = {
    val sort = BSONDocument("published" -> -1)
    super.getItems(sort = sort)
  }

  def upsertPost(id: Int, post: Post): Future[UpdateWriteResult] = {
    val selector = BSONDocument("id" -> id)
    super.upsertItem(selector, post)
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

  // Option to have a compatibility with the getPost method
  def getNewEmptyPost: Future[Option[Post]] = {
    val sort = BSONDocument("id" -> -1)
    super.getItems(sort = sort, maxItems = 1).map {
      list =>
        list.headOption match {
          case Some(post) =>
            Some(Post(post.id + 1, "New Post", "Summary", "Content", DateTime.now + 10.years))
          case None =>
            Some(Post(0, "New Post", "Summary", "Content", DateTime.now + 10.years))
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
