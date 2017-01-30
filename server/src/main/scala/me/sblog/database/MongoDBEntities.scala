package me.sblog.database

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}

object MongoDBEntities {

  case class Document(id: Int, title: String, summary: String)

  object Document {
    type Formatter[T] = BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]

    implicit val documentFormat: Formatter[Document] = Macros.handler[Document]
  }

}
