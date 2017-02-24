package me.lsbengine.api.admin.security

import akka.http.scaladsl.model.StatusCodes.Forbidden
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair}
import akka.http.scaladsl.server._
import com.github.nscala_time.time.Imports._
import me.lsbengine.database.model.Token
import me.lsbengine.server.BlogConfiguration
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global

trait CookiesAuthenticator extends Directives {
  val dbConnection: MongoConnection
  val dbName: String

  def isTokenValid(token: Token): Boolean = {
    val now = DateTime.now
    token.expiry > now
  }

  def cookieAuthenticator: Directive1[Token] = {
    optionalCookie(cookieName).flatMap {
      case Some(cookiePair) =>
        val tokenId = cookiePair.value
        onSuccess(dbConnection.database(dbName)).flatMap {
          db =>
            val tokensAccessor = new TokensAccessor(db)
            onSuccess(tokensAccessor.getTokenWithId(tokenId)).flatMap {
              case Some(token) =>
                if (isTokenValid(token)) {
                  tokenRefresh(token, db)
                  provide(token)
                } else {
                  reject(ValidationRejection("Invalid token."))
                }
              case None =>
                reject(ValidationRejection("No token."))
            }
        }
      case None =>
        reject(MissingCookieRejection(cookieName))
    }
  }

  protected def tokenRefresh(token: Token, db: DefaultDB): Unit = {
    val now = DateTime.now
    if (now + 2.days > token.expiry) {
      val newToken = TokenGenerator.renewToken(token)
      val tokensAccessor = new TokensAccessor(db)
      tokensAccessor.storeToken(newToken)
    }
  }

  protected def generateCookie(newToken: Token): HttpCookie = {
    val secure = BlogConfiguration.appContext == "PROD"
    val expires = Some(akka.http.scaladsl.model.DateTime(newToken.expiry.getMillis))
    // 2 weeks
    val maxAge = Some(2L * 7 * 24 * 60 * 60 * 1000)
    HttpCookie.fromPair(
      pair = HttpCookiePair(cookieName, newToken.tokenId),
      expires = expires,
      maxAge = maxAge,
      path = Some("/"),
      secure = secure,
      httpOnly = true)
  }

  protected def csrfCheck(token: Token)
                       (onSuccess: Route): Route = {
    optionalHeaderValueByName(csrfHeaderName) { csrfHeader =>
      if (csrfHeader.getOrElse("Invalid") == token.csrf) {
        onSuccess
      } else {
        complete(Forbidden, "CSRF Prevented")
      }
    }
  }

  protected def cookieWithCsrfCheck(onSuccess: Route): Route = {
    cookieAuthenticator { token =>
      csrfCheck(token)(onSuccess)
    }
  }

}
