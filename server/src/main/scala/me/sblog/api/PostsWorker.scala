package me.sblog.api

import akka.actor.Props
import me.sblog.api.PostsWorker.{FetchDocument, FetchPostResponse, ListAction, ListActionResponse}
import me.sblog.database.DatabaseAccessor
import me.sblog.database.MongoDBEntities.Post
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PostsWorker {

  case class ListAction()

  case class FetchDocument(id: Int)

  case class ListActionResponse(list: List[Post])

  case class FetchPostResponse(document: Post)

  def props(ctx: RequestContext, db: DefaultDB): Props = {
    Props(new PostsWorker(ctx, db))
  }

}

class PostsWorker(ctx: RequestContext, db: DefaultDB) extends ApiWorker(ctx) {
  val postsAccessor = new DatabaseAccessor[Post](db, DatabaseAccessor.postsCollectionName)

  def receive: Receive = {
    case ListAction() =>
      log.info(s"Listing posts.")
      postsAccessor.listItems.onComplete {
        case Success(list) =>
          ok(ListActionResponse(list))
        case Failure(e) =>
          internalError(e)
      }

    case FetchDocument(id) =>
      val query = BSONDocument("id" -> id)
      log.info(s"Fetching post $id.")
      postsAccessor.getItem(query).onComplete {
        case Success(maybeDocument) =>
          maybeDocument match {
            case Some(document) => ok(FetchPostResponse(document))
            case None => notFound(s"No post found with id: $id.")
          }
        case Failure(e) =>
          internalError(e)
      }
    case _ =>
      badRequest("Unknown request sent to PostsWorker.")
  }
}
