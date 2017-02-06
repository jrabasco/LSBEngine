package me.lsbengine.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory}
import me.lsbengine.api.{FetchPost, ListAction}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import reactivemongo.api.{DefaultDB, MongoConnection}
import spray.http.StatusCodes.InternalServerError
import spray.httpx.Json4sSupport
import spray.routing.{HttpService, RequestContext, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

abstract class ServerService(dbConnection: MongoConnection, dbName: String) extends HttpService with Actor with ActorLogging with Json4sSupport {
  override def actorRefFactory: ActorRefFactory = context

  override def receive: Receive = runRoute(commonRoutes ~ routes)

  implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  val commonRoutes: Route =
    path("info") {
      get {
        complete(getInfo)
      }
    } ~ pathPrefix("posts") {
      path("list") {
        get {
          ctx => listPosts(ctx)
        }
      } ~ path(IntNumber) {
        id =>
          get {
            ctx => fetchPost(ctx, id)
          }
      }
    } ~ pathPrefix("css") {
      get {
        getFromResourceDirectory("css")
      }
    } ~ pathPrefix("js") {
      get {
        getFromResourceDirectory("js")
      }
    }

  def listPosts(requestContext: RequestContext): Unit = {
    log.info(s"[$apiScope] Listing posts.")
    handleWithDb(requestContext) {
      db =>
        val postsWorker = getPostsWorker(requestContext, db)
        postsWorker ! ListAction()
    }
  }

  def fetchPost(requestContext: RequestContext, id: Int): Unit = {
    log.info(s"[$apiScope] Fetching post $id.")
    handleWithDb(requestContext) {
      db =>
        val postsWorker = getPostsWorker(requestContext, db)
        postsWorker ! FetchPost(id)
    }
  }

  def handleWithDb(reqContext: RequestContext)(handler: DefaultDB => Unit): Unit = {
    dbConnection.database(dbName).onComplete {
      case Success(db) =>
        handler(db)
      case Failure(e) =>
        reqContext.complete(InternalServerError, s"Could not connect to database: $e")
    }
  }

  val routes: Route
  val apiScope: String

  def getInfo: Map[String, Any] = {
    BuildInfo.toMap + ("repositoryLink" -> BlogConfiguration.repositoryLink) + ("apiScope" -> apiScope)
  }

  def getPostsWorker(requestContext: RequestContext, database: DefaultDB): ActorRef
}
