package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import me.lsbengine.api.PostsAccessor
import me.lsbengine.api.public.PublicPostsAccessor
import me.lsbengine.pages.public.html
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublicService(dbConnection: MongoConnection, dbName: String, log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log) {

  override val ownRoutes: Route =
    pathSingleSlash {
      ctx => index(ctx)
    }

  override val apiScope: String = "public"

  def index(reqContext: RequestContext): Future[RouteResult] = {
    handleWithDb(reqContext) {
      db =>
        val postsAccessor = getPostsAccessor(db)
        postsAccessor.listPosts.flatMap {
          list =>
            reqContext.complete(html.index.render(list))
        }.recoverWith {
          case _ =>
            reqContext.complete(html.index.render(List()))
        }
    }
  }

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new PublicPostsAccessor(database)
  }

}
