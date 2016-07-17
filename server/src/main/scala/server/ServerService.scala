package server

import akka.actor.{Actor, ActorLogging}
import api.DocumentsWorker
import api.DocumentsWorker.{FetchDocument, ListAction}
import org.json4s.{DefaultFormats, Formats}
import reactivemongo.api.DefaultDB
import spray.client.pipelining._
import spray.http.HttpHeaders.Accept
import spray.http.MediaTypes._
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.Json4sSupport
import spray.routing.{HttpService, RequestContext, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class ServerService(db: DefaultDB) extends HttpService with Actor with ActorLogging with Json4sSupport {
  override def actorRefFactory = context

  override def receive = runRoute(commonRoutes ~ routes)

  implicit def json4sFormats: Formats = DefaultFormats

  implicit val pipelineRawJson: HttpRequest => Future[HttpResponse] = (
    addHeader(Accept(`application/json`))
      ~> sendReceive
    )

  val commonRoutes =
    path("info") {
      get {
        complete(getInfo)
      }
    } ~ pathPrefix("documents") {
      path("list") {
        get {
          ctx => listDocuments(ctx)
        }
      } ~
        path(IntNumber) {
          id =>
            get {
              ctx => fetchDocument(ctx, id)
            }
        }
    }

  def listDocuments(ctx: RequestContext): Unit = {
    val documentsWorker = context.actorOf(DocumentsWorker.props(ctx, db))
    documentsWorker ! ListAction()
  }

  def fetchDocument(ctx: RequestContext, id: Int): Unit = {
    val documentsWorker = context.actorOf(DocumentsWorker.props(ctx, db))
    documentsWorker ! FetchDocument(id)
  }

  val routes: Route

  def getInfo = {
    BuildInfo.toMap + ("repositoryLink" -> ApplicationConfiguration.repositoryLink)
  }
}
