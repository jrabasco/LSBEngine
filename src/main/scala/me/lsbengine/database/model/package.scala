package me.lsbengine.database

import com.github.nscala_time.time.Imports._
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}

package object model {

  object MongoCollections {
    val postsCollectionName = "posts"
    val projectsCollectionName = "projects"
    val aboutMeCollectionName = "aboutMe"
    val navBarConfCollectionName = "navBarConf"
    val usersCollectionName = "users"
    val tokensCollectionName = "tokens"
    val postsTrashCollectionName = "postsTrash"
    val projectsTrashCollectionName = "projectsTrash"
  }

  case class HtmlMarkdownContent(html: String, markdown: String)

  case class Post(id: Int, title: String, `abstract`: String, content: HtmlMarkdownContent, published: DateTime)

  case class Project(id: Int, title: String, `abstract`: String, content: HtmlMarkdownContent, published: DateTime)

  case class AboutMe(introduction: Option[HtmlMarkdownContent], resume: Option[HtmlMarkdownContent])

  case class NavBarConf(projects: Boolean, about: Boolean)

  case class User(userName: String, password: String, salt: String)

  case class Token(tokenId: String, userName: String, csrf: String, expiry: DateTime)

  object MongoFormats {
    type Formatter[T] = BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)

      def write(dateTime: DateTime) = BSONDateTime(dateTime.getMillis)
    }

    implicit val htmlMarkdownContentFormat: Formatter[HtmlMarkdownContent] = Macros.handler[HtmlMarkdownContent]

    implicit val postFormat: Formatter[Post] = Macros.handler[Post]
    implicit val projectFormat: Formatter[Project] = Macros.handler[Project]
    implicit val aboutMeFormat: Formatter[AboutMe] = Macros.handler[AboutMe]
    implicit val navBarFormat: Formatter[NavBarConf] = Macros.handler[NavBarConf]
    implicit val userFormat: Formatter[User] = Macros.handler[User]
    implicit val tokenFormat: Formatter[Token] = Macros.handler[Token]
  }

}
