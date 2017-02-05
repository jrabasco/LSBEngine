package me.lsbengine.api.admin.security

import com.github.nscala_time.time.Imports._
import me.lsbengine.database.model.Token
import reactivemongo.api.{DefaultDB, MongoConnection}
import spray.http.HttpCookie
import spray.routing.authentication.{Authentication, ContextAuthenticator}
import spray.routing.{MissingCookieRejection, ValidationRejection}

import scala.concurrent.{ExecutionContext, Future}

trait CookiesAuthenticator {
  val dbConnection: MongoConnection
  val dbName: String

  def isTokenValid(token: Token): Boolean = {
    val now = DateTime.now
    token.expiry > now
  }

  def cookieAuthenticator(implicit ec: ExecutionContext): ContextAuthenticator[Token] = ctx => {
    val cookies = ctx.request.cookies
    val maybeAuthCookie = cookies.find(_.name == cookieName)
    val futureCredMissingRejection: Future[Authentication[Token]] = Future(Left(MissingCookieRejection(cookieName)))
    maybeAuthCookie.fold(futureCredMissingRejection) {
      authCookie: HttpCookie =>
        val tokenId = authCookie.content
        dbConnection.database(dbName).flatMap {
          db =>
            val tokensAccessor = new TokensAccessor(db)
            tokensAccessor.getTokenWithId(tokenId).map {
              maybeToken =>
                maybeToken.fold[Authentication[Token]](Left(ValidationRejection("No token."))) {
                  token =>
                    if (isTokenValid(token)) {
                      tokenRefresh(token, db)
                      Right(token)
                    } else {
                      Left(ValidationRejection("Invalid token."))
                    }
                }
            }
        }
    }
  }

  def tokenRefresh(token: Token, db: DefaultDB): Unit = {
    val now = DateTime.now
    if (now + 2.days > token.expiry) {
      val newToken = TokenGenerator.renewToken(token)
      val tokensAccessor = new TokensAccessor(db)
      tokensAccessor.storeToken(newToken)
    }
  }
}
