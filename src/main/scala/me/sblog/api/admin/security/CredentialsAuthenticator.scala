package me.sblog.api.admin.security

import me.sblog.database.DatabaseAccessor
import me.sblog.database.model.{MongoCollections, User}
import reactivemongo.api.MongoConnection
import reactivemongo.bson.BSONDocument
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{ExecutionContext, Future}

object CredentialsAuthenticator {

  case class Credentials(userName: String, password: String)

}

trait CredentialsAuthenticator {

  val dbConnection: MongoConnection
  val dbName: String

  def isPasswordValid(actual: String, salt: String, provided: String): Boolean = {
    val decodedSalt = base64Decode(salt)
    val decodedActual = base64Decode(actual)
    val hashedProvided = PasswordHasher.hashPassword(provided, decodedSalt)
    val sameLength = decodedActual.length == hashedProvided.length
    val sameContent = decodedActual.zip(hashedProvided).forall {
      case (actByte, provByte) => actByte == provByte
    }
    sameLength && sameContent
  }

  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[User] = {
    def validateUser(maybeUserPass: Option[UserPass]): Future[Option[User]] = {
      val noneUser: Option[User] = None
      maybeUserPass.fold(Future(noneUser)) {
        userPass =>
          dbConnection.database(dbName).flatMap {
            db =>
              val usersAccessor = new DatabaseAccessor[User](db, MongoCollections.usersCollectionName)
              val selector = BSONDocument("userName" -> userPass.user)
              usersAccessor.getItem(selector).map {
                maybeUser =>
                  maybeUser.fold {
                    //Does not give away that the username is invalid, update with appropriate time once number of
                    // iterations is decided on
                    Thread.sleep(1000)
                    noneUser
                  } {
                    user =>
                      if (isPasswordValid(user.password, user.salt, userPass.pass)) {
                        Some(user)
                      } else {
                        None
                      }
                  }
              }
          }
      }

    }

    def authenticator(userPass: Option[UserPass]): Future[Option[User]] = validateUser(userPass)

    BasicAuth(authenticator _, realm = "Admin API")
  }
}
