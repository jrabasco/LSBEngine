package me.sblog.api.admin.security

import com.github.nscala_time.time.Imports._
import me.sblog.database.model.Token
import reactivemongo.api.MongoConnection
import spray.http.HttpCookie
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.authentication.{Authentication, ContextAuthenticator}

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
    val futureCredMissingRejection: Future[Authentication[Token]] = Future(Left(AuthenticationFailedRejection(CredentialsMissing, List())))
    maybeAuthCookie.fold(futureCredMissingRejection) {
      authCookie: HttpCookie =>
        val tokenId = authCookie.content
        dbConnection.database(dbName).flatMap {
          db =>
            val tokensAccessor = new TokensAccessor(db)
            tokensAccessor.getTokenWithId(tokenId).map {
              maybeToken =>
                val rejection = Left(AuthenticationFailedRejection(CredentialsRejected, List()))
                maybeToken.fold[Authentication[Token]](rejection) {
                  token =>
                    if (isTokenValid(token)) {
                      Right(token)
                    } else {
                      rejection
                    }
                }
            }
        }
    }
  }
}
