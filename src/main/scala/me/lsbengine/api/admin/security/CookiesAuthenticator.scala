package me.lsbengine.api.admin.security

import akka.http.scaladsl.server.{Directive1, Directives, MissingCookieRejection, ValidationRejection}
import com.github.nscala_time.time.Imports._
import me.lsbengine.database.model.Token
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

  def tokenRefresh(token: Token, db: DefaultDB): Unit = {
    val now = DateTime.now
    if (now + 2.days > token.expiry) {
      val newToken = TokenGenerator.renewToken(token)
      val tokensAccessor = new TokensAccessor(db)
      tokensAccessor.storeToken(newToken)
    }
  }
}
