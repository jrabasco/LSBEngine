package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{NotFound, InternalServerError}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import me.lsbengine.api.PostsAccessor
import me.lsbengine.api.public.PublicPostsAccessor
import me.lsbengine.pages.public.html
import me.lsbengine.errors
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublicService(dbConnection: MongoConnection, dbName: String, log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log) {

  override val ownRoutes: Route =
    pathSingleSlash {
      ctx => index(ctx)
    } ~ pathPrefix("posts") {
      path(IntNumber) {
        id =>
          ctx => individualPost(ctx, id)
      }
    }

  override val apiScope: String = "public"

  def index(requestContext: RequestContext): Future[RouteResult] = {
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = getPostsAccessor(db)
        postsAccessor.listPosts.flatMap {
          list =>
            requestContext.complete(html.index.render(list))
        }.recoverWith {
          case _ =>
            requestContext.complete(html.index.render(List()))
        }
    }
  }

  def individualPost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = getPostsAccessor(db)
        postsAccessor.getPost(id).flatMap {
          case Some(post) =>
            requestContext.complete(html.post.render(post))
          case None =>
            requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id not found."))
        }.recoverWith {
          case _ =>
            requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
        }
    }
  }

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new PublicPostsAccessor(database)
  }

}
