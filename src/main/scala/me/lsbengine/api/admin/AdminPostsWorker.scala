package me.lsbengine.api.admin

import akka.actor.Props
import me.lsbengine.api._
import me.lsbengine.api.admin.AdminPostsWorker._
import me.lsbengine.database.model.Post
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object AdminPostsWorker {

  case class UpsertPost(id: Int, post: Post)

  def props(ctx: RequestContext, db: DefaultDB): Props = {
    Props(new AdminPostsWorker(ctx, db))
  }

}

class AdminPostsWorker(ctx: RequestContext, db: DefaultDB) extends ApiWorker(ctx) {
  val postsAccessor = new AdminPostsAccessor(db)

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
      badRequest("Unknown request sent to AdminPostsWorker.")
  }
}