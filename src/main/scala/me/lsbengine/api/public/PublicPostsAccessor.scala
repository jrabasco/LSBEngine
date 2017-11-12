package me.lsbengine.api.public

import com.github.nscala_time.time.Imports.DateTime
import me.lsbengine.api.PostsAccessor
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import me.lsbengine.database.model.MongoFormats._
import me.lsbengine.server.BlogConfiguration
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

  def listPosts(category: Option[String], pageOpt: Option[Int] = None, postsPerPageOpt: Option[Int] = None): Future[List[Post]] = {
    val now = DateTime.now
    val sort = BSONDocument("published" -> -1)
    val page = pageOpt.getOrElse(1)
    val postsPerPage = postsPerPageOpt.getOrElse(BlogConfiguration.defaultPostsPerPage)
    val query = 
      BSONDocument(
          "published" -> BSONDocument(
            "$lte" -> BSONDateTime(now.getMillis)
        )) ++ category.fold(BSONDocument())(cat => BSONDocument("category" -> cat))
    val skip = (page - 1) * postsPerPage
    super.getItems(query = query, sort = sort, skip = skip, maxItems = postsPerPage)
  }
  
}
