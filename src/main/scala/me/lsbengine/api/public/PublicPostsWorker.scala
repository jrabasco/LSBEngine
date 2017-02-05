package me.lsbengine.api.public

import akka.actor.Props
import me.lsbengine.api._
import reactivemongo.api.DefaultDB
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PublicPostsWorker {
  def props(ctx: RequestContext, db: DefaultDB): Props = {
    Props(new PublicPostsWorker(ctx, db))
  }

}

class PublicPostsWorker(ctx: RequestContext, db: DefaultDB) extends ApiWorker(ctx) {
  val postsAccessor = new PublicPostsAccessor(db)

  def receive: Receive = {
    case ListAction() =>
      postsAccessor.listPosts.onComplete {
        case Success(list) =>
          ok(ListActionResponse(list))
        case Failure(e) =>
          internalError(e)
      }

    case FetchPost(id) =>
      postsAccessor.getPost(id).onComplete {
        case Success(maybeDocument) =>
          maybeDocument match {
            case Some(document) => ok(FetchPostResponse(document))
            case None => notFound(s"No post found with id: $id.")
          }
        case Failure(e) =>
          internalError(e)
      }

    case _ =>
      badRequest("Unknown request sent to PublicPostsWorker.")
  }
}
