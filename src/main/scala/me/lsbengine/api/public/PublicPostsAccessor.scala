package me.lsbengine.api.public

import com.github.nscala_time.time.Imports.DateTime
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader}

import scala.concurrent.Future

class PublicPostsAccessor(db: DefaultDB) extends DatabaseAccessor[Post](db, MongoCollections.postsCollectionName) {

  def getPost(id: Int)(implicit reader: BSONDocumentReader[Post]): Future[Option[Post]] = {
    val now = DateTime.now
    val query = BSONDocument("id" -> id,
      "published" -> BSONDocument(
        "$lte" -> BSONDateTime(now.getMillis)
      ))
    super.getItem(query)
  }

  def listPosts()(implicit reader: BSONDocumentReader[Post]): Future[List[Post]] = {
    val now = DateTime.now
    val sort = BSONDocument("published" -> -1)
    val query = BSONDocument("published" -> BSONDocument(
      "$lte" -> BSONDateTime(now.getMillis)
    ))
    super.getItems(query = query, sort = sort)
  }
}
