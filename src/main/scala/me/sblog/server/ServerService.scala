package me.sblog.server

import akka.actor.{Actor, ActorLogging, ActorRefFactory}
import me.sblog.api.public.PostsWorker
import me.sblog.api.public.PostsWorker.{FetchDocument, ListAction}
import org.json4s.{DefaultFormats, Formats}
import reactivemongo.api.{DefaultDB, MongoConnection}
import spray.client.pipelining._
import spray.http.HttpHeaders.Accept
import spray.http.MediaTypes._
import spray.http.StatusCodes.InternalServerError
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.Json4sSupport
import spray.routing.{HttpService, RequestContext, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

abstract class ServerService(dbConnection: MongoConnection, dbName: String) extends HttpService with Actor with ActorLogging with Json4sSupport {
  override def actorRefFactory: ActorRefFactory = context

  override def receive: Receive = runRoute(commonRoutes ~ routes)

  implicit def json4sFormats: Formats = DefaultFormats

  implicit val pipelineRawJson: HttpRequest => Future[HttpResponse] = (
    addHeader(Accept(`application/json`))
      ~> sendReceive
    )

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

  def listPosts(reqContext: RequestContext): Unit = {
    log.info(s"[$apiScope] Listing posts.")
    handleWithDb(reqContext) {
      db =>
        val postsWorker = context.actorOf(PostsWorker.props(reqContext, db))
        postsWorker ! ListAction()
    }
  }

  def fetchPost(reqContext: RequestContext, id: Int): Unit = {
    log.info(s"[$apiScope] Fetching post $id.")
    handleWithDb(reqContext) {
      db =>
        val postsWorker = context.actorOf(PostsWorker.props(reqContext, db))
        postsWorker ! FetchDocument(id)
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
    BuildInfo.toMap + ("repositoryLink" -> ApplicationConfiguration.repositoryLink) + ("apiScope" -> apiScope)
  }
}
