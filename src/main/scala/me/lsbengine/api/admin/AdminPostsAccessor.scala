package me.lsbengine.api.admin

import me.lsbengine.api.PostsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import me.lsbengine.database.model.MongoFormats._
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

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
}
