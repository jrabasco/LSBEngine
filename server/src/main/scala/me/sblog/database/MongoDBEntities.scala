package database

import reactivemongo.bson.Macros

object MongoDBEntities {

  case class Document(id: Int, title: String, summary: String)

  object Document {
    implicit val documentFormat = Macros.handler[Document]
  }

}
