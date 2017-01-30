package me.sblog.server

import akka.actor.Props
import com.github.nscala_time.time.Imports._
import me.sblog.api.admin.security._
import me.sblog.database.model.Token
import reactivemongo.api.MongoConnection
import spray.http.HttpCookie
import spray.http.StatusCodes.InternalServerError
import spray.routing.Route

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object AdminService {
  def props(dbConnection: MongoConnection, dbName: String): Props =
    Props(new AdminService(dbConnection, dbName))
}

class AdminService(val dbConnection: MongoConnection, val dbName: String)
  extends ServerService(dbConnection, dbName)
    with CredentialsAuthenticator
    with CookiesAuthenticator {

  override val apiScope: String = "admin"

  override val routes: Route =
    pathPrefix("api") {
      authenticate(basicUserAuthenticator) { user =>
        path("token") {
          log.info(s"[$apiScope] Getting token for user ${user.userName}.")
          get {
            onComplete(dbConnection.database(dbName)) {
              case Success(db) =>
                val tokensAccessor = new TokensAccessor(db)
                onComplete(tokensAccessor.getTokenWithUserName(user.userName)) {
                  case Success(maybeToken) =>
                    val newToken = maybeToken match {
                      case Some(oldToken) =>
                        tokenRefresh(oldToken)
                      case None =>
                        TokenGenerator.generateToken(user.userName)
                    }
                    tokensAccessor.storeToken(newToken)
                    val secure = ApplicationConfiguration.appContext == "PROD"
                    val expires = Some(spray.http.DateTime(newToken.expiry.getMillis))
                    val maxAge = Some(2L * 7 * 24 * 60 * 60 * 1000)
                    val cookie = HttpCookie(cookieName,
                      content = newToken.tokenId,
                      expires = expires,
                      maxAge = maxAge,
                      path = Some("/api"),
                      secure = secure,
                      httpOnly = true)
                    setCookie(cookie) {
                      complete(s"Welcome ${user.userName}.")
                    }
                  case Failure(e) =>
                    complete(InternalServerError, s"Could not generate token: $e")
                }
              case Failure(e) =>
                complete(InternalServerError, s"Could not generate token: $e")
            }
          }
        }
      } ~ authenticate(cookieAuthenticator) { token =>
        path("check") {
          log.info(s"[$apiScope] Checking token for user ${token.userName}.")
          get {
            val now = DateTime.now
            if (now + 2.days > token.expiry) {
              val newToken = TokenGenerator.renewToken(token)
              dbConnection.database(dbName).map {
                db =>
                  val tokensAccessor = new TokensAccessor(db)
                  tokensAccessor.storeToken(newToken)
              }
            }
            complete(s"User ${token.userName} is logged in.")
          }
        }
      } ~ authenticate(cookieAuthenticator) { token =>
        path("logout") {
          log.info(s"[$apiScope] Logging out user ${token.userName}.")
          deleteCookie(cookieName) {
            dbConnection.database(dbName).map {
              db =>
                val tokensAccessor = new TokensAccessor(db)
                tokensAccessor.removeToken(token.tokenId)
            }
            complete("Successfully logged out.")
          }
        }
      }
    }

  def tokenRefresh(token: Token): Token = {
    val now = DateTime.now
    if (now + 2.days > token.expiry) {
      TokenGenerator.renewToken(token)
    } else {
      token
    }
  }

}
