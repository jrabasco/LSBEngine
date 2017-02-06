package me.lsbengine.server

import akka.actor.{ActorRef, Props}
import me.lsbengine.api.public.{PublicPostsAccessor, PublicPostsWorker}
import reactivemongo.api.{DefaultDB, MongoConnection}
import spray.httpx.PlayTwirlSupport._
import me.lsbengine.pages.public.html
import spray.routing.{RequestContext, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PublicService {
  def props(dbConnection: MongoConnection, dbName: String): Props =
    Props(new PublicService(dbConnection, dbName))
}

class PublicService(dbConnection: MongoConnection, dbName: String) extends ServerService(dbConnection, dbName) {

  override val routes: Route =
    pathSingleSlash {
      ctx => index(ctx)
    }

  override val apiScope: String = "public"

  def index(reqContext: RequestContext): Unit = {
    handleWithDb(reqContext) {
      db =>
        val postsAccessor = new PublicPostsAccessor(db)
        postsAccessor.listPosts.onComplete {
          case Success(list) =>
            reqContext.complete(html.index.render(list))
          case Failure(_) =>
            reqContext.complete(html.index.render(List()))
        }
    }
  }

  override def getPostsWorker(requestContext: RequestContext, database: DefaultDB): ActorRef = {
    context.actorOf(PublicPostsWorker.props(requestContext, database))
  }

}
