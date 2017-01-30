package me.sblog.api

import akka.actor.Props
import me.sblog.api.DocumentsWorker.{FetchDocument, FetchDocumentResponse, ListAction, ListActionResponse}
import me.sblog.database.DatabaseAccessor
import me.sblog.database.MongoDBEntities.Document
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object DocumentsWorker {

  case class ListAction()

  case class FetchDocument(id: Int)

  case class ListActionResponse(list: List[Document])

  case class FetchDocumentResponse(document: Document)

  def props(ctx: RequestContext, db: DefaultDB): Props = {
    Props(new DocumentsWorker(ctx, db))
  }

}

class DocumentsWorker(ctx: RequestContext, db: DefaultDB) extends ApiWorker(ctx) {
  val documentsAccessor = new DatabaseAccessor[Document](db, DatabaseAccessor.documentsCollectionName)

  def receive: Receive = {
    case ListAction() =>
      documentsAccessor.listItems.onComplete {
        case Success(list) =>
          ok(ListActionResponse(list))
        case Failure(e) =>
          internalError(e)
      }

    case FetchDocument(id) =>
      val query = BSONDocument("id" -> id)
      documentsAccessor.getItem(query).onComplete {
        case Success(maybeDocument) =>
          maybeDocument match {
            case Some(document) => ok(FetchDocumentResponse(document))
            case None => notFound(s"No document found with id: $id.")
          }
        case Failure(e) =>
          internalError(e)
      }
    case _ =>
      badRequest("Unknown request sent to DocumentsWorker.")
  }
}
