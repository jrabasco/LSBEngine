package me.lsbengine.server

import akka.actor.Props
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Post}
import reactivemongo.api.MongoConnection
import spray.httpx.PlayTwirlSupport._
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
        val postsAccessor = new DatabaseAccessor[Post](db, MongoCollections.postsCollectionName)
        postsAccessor.listItems.onComplete {
          case Success(list) =>
            reqContext.complete(html.index.render(list))
          case Failure(e) =>
            reqContext.complete(html.index.render(List()))
        }
    }
  }

}
