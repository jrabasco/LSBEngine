package me.lsbengine.database

import com.github.nscala_time.time.Imports._
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}

package object model {

  object MongoCollections {
    val postsCollectionName = "posts"
    val navBarConfCollection = "navBarConf"
    val usersCollectionName = "users"
    val tokensCollectionName = "tokens"
    val trashCollectionName = "trash"
  }


  case class Post(id: Int, title: String, summary: String, contentMarkdown: String, contentHtml: String, published: DateTime)

  case class NavBarConf(projects: Boolean, about: Boolean)

  case class User(userName: String, password: String, salt: String)

  case class Token(tokenId: String, userName: String, csrf: String, expiry: DateTime)

  object MongoFormats {
    type Formatter[T] = BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)

      def write(dateTime: DateTime) = BSONDateTime(dateTime.getMillis)
    }

    implicit val postFormat: Formatter[Post] = Macros.handler[Post]
    implicit val navBarFormat: Formatter[NavBarConf] = Macros.handler[NavBarConf]
    implicit val userFormat: Formatter[User] = Macros.handler[User]
    implicit val tokenFormat: Formatter[Token] = Macros.handler[Token]
  }

}
