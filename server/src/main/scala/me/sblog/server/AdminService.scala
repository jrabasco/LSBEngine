package server

import akka.actor.Props
import reactivemongo.api.DefaultDB
import spray.http.StatusCodes.NotImplemented

object AdminService {
  def props(db: DefaultDB): Props =
    Props(new AdminService(db))
}

class AdminService(db: DefaultDB) extends ServerService(db) {

  override val routes =
    path("token") {
      get {
        complete(NotImplemented, "Cannot get a token yet.")
      }
    }

  override def getInfo = {
    super.getInfo + ("apiScope" -> "admin")
  }

}
