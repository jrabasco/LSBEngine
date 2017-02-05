package me.lsbengine.api.admin

import com.github.nscala_time.time.Imports.DateTime
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader}

import scala.concurrent.Future

class AdminPostsAccessor(db: DefaultDB) extends DatabaseAccessor[Post](db, MongoCollections.postsCollectionName){
  def getPost(id: Int)(implicit reader: BSONDocumentReader[Post]): Future[Option[Post]] = {
    val query = BSONDocument("id" -> id)
    super.getItem(query)
  }

  def listPosts()(implicit reader: BSONDocumentReader[Post]): Future[List[Post]] = {
    val sort = BSONDocument("published" -> -1)
    super.getItems(sort = sort)
  }
}
