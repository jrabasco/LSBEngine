package me.lsbengine.api.admin

import com.github.nscala_time.time.Imports._
import me.lsbengine.api.PostsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.MongoFormats._
import me.lsbengine.database.model.{MongoCollections, Post}
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminPostsAccessor(db: DefaultDB)
  extends DatabaseAccessor[Post](db, MongoCollections.postsCollectionName)
    with PostsAccessor {
  def getPost(id: Int): Future[Option[Post]] = {
    val query = BSONDocument("id" -> id)
    super.getItem(query)
  }

  def listPosts: Future[List[Post]] = {
    val sort = BSONDocument("published" -> -1)
    super.getItems(sort = sort)
  }

  //Wrapper which passes the implicit formatter imported from database.model.MongoFormats
  def upsertPost(selector: BSONDocument, post: Post): Future[UpdateWriteResult] = {
    super.upsertItem(selector, post)
  }

  // Option to have a compatibility with the getPost method
  def getNewEmptyPost: Future[Option[Post]] = {
    val sort = BSONDocument("id" -> -1)
    super.getItems(sort = sort, maxItems = 1).map {
      list =>
        list.headOption match {
          case Some(post) =>
            Some(Post(post.id + 1, "New Post", "Summary", "Content", DateTime.now))
          case None =>
            Some(Post(0, "New Post", "Summary", "Content", DateTime.now))
        }
    }
  }
}
