package server

import akka.actor.Props
import reactivemongo.api.DefaultDB
import spray.http.StatusCodes._

object PublicService {
  def props(db: DefaultDB): Props =
    Props(new PublicService(db))
}

class PublicService(db: DefaultDB) extends ServerService(db) {

  override val routes =
    path("token") {
      get {
        complete(BadRequest, "No token on public api.")
      }
    }

  override def getInfo = {
    super.getInfo + ("apiScope" -> "public")
  }

}
