package me.sblog.server

import akka.actor.Props
import reactivemongo.api.MongoConnection
import spray.http.StatusCodes.NotImplemented
import spray.routing.Route

object AdminService {
  def props(dbConnection: MongoConnection, dbName: String): Props =
    Props(new AdminService(dbConnection, dbName))
}

class AdminService(dbConnection: MongoConnection, dbName: String) extends ServerService(dbConnection, dbName) {

  override val routes: Route =
    path("token") {
      get {
        complete(NotImplemented, "Cannot get a token yet.")
      }
    }

  override val apiScope: String = "admin"

}
