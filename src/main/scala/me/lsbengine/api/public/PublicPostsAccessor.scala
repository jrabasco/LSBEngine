package me.lsbengine.api.public

import com.github.nscala_time.time.Imports.DateTime
import me.lsbengine.api.PostsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import me.lsbengine.database.model.MongoFormats._
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDateTime, BSONDocument}

import scala.concurrent.Future

class PublicPostsAccessor(db: DefaultDB)
  extends DatabaseAccessor[Post](db, MongoCollections.postsCollectionName)
    with PostsAccessor {

  def getPost(id: Int): Future[Option[Post]] = {
    val now = DateTime.now
    val query = BSONDocument("id" -> id,
      "published" -> BSONDocument(
        "$lte" -> BSONDateTime(now.getMillis)
      ))
    super.getItem(query)
  }

  def listPosts(category: Option[String] = None): Future[List[Post]] = {
    val now = DateTime.now
    val sort = BSONDocument("published" -> -1)
    val query = category match {
      case Some(cat) =>
        BSONDocument(
          "category" -> cat,
          "published" -> BSONDocument(
            "$lte" -> BSONDateTime(now.getMillis)
        ))
      case None =>
        BSONDocument(
          "published" -> BSONDocument(
            "$lte" -> BSONDateTime(now.getMillis)
        ))
    }
    super.getItems(query = query, sort = sort)
  }
  
}
