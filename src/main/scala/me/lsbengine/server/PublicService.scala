package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import me.lsbengine.api.PostsAccessor
import me.lsbengine.api.public.PublicPostsAccessor
import me.lsbengine.errors
import me.lsbengine.pages.public.html
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublicService(dbConnection: MongoConnection, dbName: String, log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log) {

  override val ownRoutes: Route =
    pathSingleSlash {
      ctx => index(ctx)
    } ~ pathPrefix("posts") {
      path(IntNumber) { id =>
        ctx => individualPost(ctx, id)
      }
    } ~ pathPrefix("about") {
      ctx => about(ctx)
    } ~ pathPrefix("projects") {
      pathEndOrSingleSlash {
        ctx => projects(ctx)
      } ~ path(IntNumber) { id =>
        ctx => individualProject(ctx, id)
      }
    }

  override val apiScope: String = "public"

  def index(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val postsAccessor = getPostsAccessor(db)
      postsAccessor.listPosts.flatMap {
        list =>
          requestContext.complete(html.index.render(list, conf))
      }.recoverWith {
        case _ =>
          requestContext.complete(html.index.render(List(), conf))
      }
    }
  }

  def individualPost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val postsAccessor = getPostsAccessor(db)
      postsAccessor.getPost(id).flatMap {
        case Some(post) =>
          requestContext.complete(html.post.render(post, conf))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id not found."))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  def about(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      requestContext.complete(html.about.render(conf))
    }
  }

  def projects(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      requestContext.complete(html.projects.render(conf))
    }
  }

  def individualProject(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      requestContext.complete(html.project.render(id, conf))
    }
  }

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new PublicPostsAccessor(database)
  }

}
