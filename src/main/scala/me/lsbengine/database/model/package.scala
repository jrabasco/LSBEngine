package me.lsbengine.database

import org.joda.time.DateTime
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}

package object model {

  object MongoCollections {
    val postsCollectionName = "posts"
    val usersCollectionName = "users"
    val tokensCollectionName = "tokens"
  }

  type Formatter[T] = BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)

    def write(dateTime: DateTime) = BSONDateTime(dateTime.getMillis)
  }


  case class Post(id: Int, title: String, summary: String)

  object Post {
    implicit val postFormat: Formatter[Post] = Macros.handler[Post]
  }

  case class PostForm(id: Int, title: String, summary: String, csrf: String)

  case class User(userName: String, password: String, salt: String)

  object User {
    implicit val userFormat: Formatter[User] = Macros.handler[User]
  }

  case class Token(tokenId: String, userName: String, csrf: String, expiry: DateTime)

  object Token {
    implicit val tokenFormat: Formatter[Token] = Macros.handler[Token]
  }

}
