package me.lsbengine.api.admin.security

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, Directives}
import me.lsbengine.api.admin.security.CredentialsAuthenticator.Credentials
import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, User}
import me.lsbengine.database.model.MongoFormats.userFormat
import reactivemongo.api.MongoConnection
import reactivemongo.bson.{BSONDocument, BSONRegex}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global

object CredentialsAuthenticator {

  case class Credentials(username: String, password: String)

}

trait CredentialsAuthenticator extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  val dbConnection: MongoConnection
  val dbName: String

  implicit val credentialsFormat: RootJsonFormat[Credentials] = jsonFormat2(Credentials)

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

  def credentialsAuthenticator: Directive1[User] = {
    entity(as[Credentials]).flatMap {
      creds =>
        onSuccess(dbConnection.database(dbName)).flatMap {
          db =>
            val usersAccessor = new DatabaseAccessor[User](db, MongoCollections.usersCollectionName)
            val selector = BSONDocument("userName" -> BSONRegex(s"^${creds.username}$$", "i"))
            onSuccess(usersAccessor.getItem(selector)).flatMap {
              case Some(user) =>
                if (isPasswordValid(user.password, user.salt, creds.password)) {
                  provide(user)
                } else {
                  reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Nope", "admin access")))
                }
              case None =>
                //Does not give away that the username is invalid because of timing
                val dummy = base64Encode(Array[Byte](22, 22, 22))
                isPasswordValid(dummy, dummy, dummy)
                reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Nope", "admin access")))
            }
        }
    }
  }
}
