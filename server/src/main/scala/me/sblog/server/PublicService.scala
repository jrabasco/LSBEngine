package me.sblog.server

import akka.actor.Props
import reactivemongo.api.MongoConnection
import spray.http.StatusCodes._
import spray.routing.Route

object PublicService {
  def props(dbConnection: MongoConnection, dbName: String): Props =
    Props(new PublicService(dbConnection, dbName))
}

class PublicService(dbConnection: MongoConnection, dbName: String) extends ServerService(dbConnection, dbName) {

  override val routes: Route =
    path("token") {
      get {
        complete(BadRequest, "No token on public api.")
      }
    }

  override def getInfo: Map[String, Any] = {
    super.getInfo + ("apiScope" -> "public")
  }

}
