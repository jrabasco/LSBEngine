package me.sblog.api.public

import akka.actor.Props
import me.sblog.api.ApiWorker
import me.sblog.api.public.PostsWorker._
import me.sblog.database.DatabaseAccessor
import me.sblog.database.model.{MongoCollections, Post}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PostsWorker {

  case class ListAction()

  case class FetchDocument(id: Int)

  case class ListActionResponse(list: List[Post])

  case class FetchPostResponse(post: Post)

  case class UpsertPost(id: Int, post: Post)

  def props(ctx: RequestContext, db: DefaultDB): Props = {
    Props(new PostsWorker(ctx, db))
  }

}

class PostsWorker(ctx: RequestContext, db: DefaultDB) extends ApiWorker(ctx) {
  val postsAccessor = new DatabaseAccessor[Post](db, MongoCollections.postsCollectionName)

  def receive: Receive = {
    case ListAction() =>
      postsAccessor.listItems.onComplete {
        case Success(list) =>
          ok(ListActionResponse(list))
        case Failure(e) =>
          internalError(e)
      }

    case FetchDocument(id) =>
      val query = BSONDocument("id" -> id)
      postsAccessor.getItem(query).onComplete {
        case Success(maybeDocument) =>
          maybeDocument match {
            case Some(document) => ok(FetchPostResponse(document))
            case None => notFound(s"No post found with id: $id.")
          }
        case Failure(e) =>
          internalError(e)
      }

    case UpsertPost(id, post) =>
      val selector = BSONDocument("id" -> id)
      postsAccessor.upsertItem(selector, post).onComplete {
        case Success(updateWriteResult) =>
          if (updateWriteResult.ok) {
            ok("Updated.")
          } else {
            internalError("Write result is not ok.")
          }
        case Failure(e) => internalError(e)
      }
    case _ =>
      badRequest("Unknown request sent to PostsWorker.")
  }
}
