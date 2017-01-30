package me.sblog.database

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}

package object model {

  case class Post(id: Int, title: String, summary: String)

  object Post {
    type Formatter[T] = BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]

    implicit val documentFormat: Formatter[Post] = Macros.handler[Post]
  }

}
