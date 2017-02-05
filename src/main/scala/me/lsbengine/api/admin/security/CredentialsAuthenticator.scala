package me.lsbengine.api.admin.security

import me.lsbengine.api.admin.security.CredentialsAuthenticator.Credentials
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, User}
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import reactivemongo.api.MongoConnection
import reactivemongo.bson.BSONDocument
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.authentication.{Authentication, ContextAuthenticator}

import scala.concurrent.{ExecutionContext, Future}

object CredentialsAuthenticator {

  case class Credentials(username: String, password: String)

}

trait CredentialsAuthenticator {

  val dbConnection: MongoConnection
  val dbName: String

  implicit val formats: Formats = DefaultFormats

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

  def credentialsAuthenticator(implicit ec: ExecutionContext): ContextAuthenticator[User] = ctx => {
    val postData = ctx.request.entity.asString
    val json = parse(postData)
    val maybeCreds = json.extractOpt[Credentials]
    val missingCreds: Future[Authentication[User]] = Future(Left(AuthenticationFailedRejection(CredentialsMissing, List())))
    maybeCreds.fold(missingCreds) {
      creds =>
        dbConnection.database(dbName).flatMap {
          db =>
            val usersAccessor = new DatabaseAccessor[User](db, MongoCollections.usersCollectionName)
            val selector = BSONDocument("userName" -> creds.username)
            usersAccessor.getItem(selector).map {
              maybeUser =>
                maybeUser.fold[Authentication[User]] {
                  //Does not give away that the username is invalid because of timing
                  val dummy = base64Encode(Array[Byte](22, 22, 22))
                  isPasswordValid(dummy, dummy, dummy)
                  Left(AuthenticationFailedRejection(CredentialsRejected, List()))
                } {
                  user =>
                    if (isPasswordValid(user.password, user.salt, creds.password)) {
                      Right(user)
                    } else {
                      Left(AuthenticationFailedRejection(CredentialsRejected, List()))
                    }
                }
            }
        }
    }
  }
}
