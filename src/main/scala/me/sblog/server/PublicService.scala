package me.sblog.server

import akka.actor.Props
import me.sblog.database.DatabaseAccessor
import me.sblog.database.model.Post
import reactivemongo.api.MongoConnection
import spray.http.StatusCodes._
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
    } ~ pathPrefix("css") {
      get {
        getFromResourceDirectory("css")
      }
    }

  override val apiScope: String = "public"

  def index(reqContext: RequestContext): Unit = {
    handleWithDb(reqContext) {
      (db, ctx) =>
        val postsAccessor = new DatabaseAccessor[Post](db, DatabaseAccessor.postsCollectionName)
        postsAccessor.listItems.onComplete {
          case Success(list) =>
            ctx.complete(html.index.render(list))
          case Failure(e) =>
            ctx.complete(html.index.render(List()))
        }
    }
  }

}
