package me.lsbengine.server

import akka.actor.Props
import com.github.nscala_time.time.Imports._
import me.lsbengine.api.admin.security.{cookieName, _}
import me.lsbengine.api.public.PostsWorker
import me.lsbengine.api.public.PostsWorker.UpsertPost
import me.lsbengine.database.model.{Post, PostForm, Token}
import reactivemongo.api.MongoConnection
import spray.http.HttpCookie
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.httpx.PlayTwirlSupport._
import spray.routing._

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
    pathSingleSlash {
      handleRejections(loginRejectionHandler) {
        authenticate(cookieAuthenticator) {
          token => complete(html.adminhome.render(token))
        }
      }
    } ~
      pathPrefix("api") {
        path("token") {
          authenticate(credentialsAuthenticator) { user =>
            log.info(s"[$apiScope] Getting token for user ${user.userName}.")
            post {
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
                        path = Some("/"),
                        secure = secure,
                        httpOnly = true)
                      respondWithHeader(`Access-Control-Allow-Credentials`(true)) {
                        setCookie(cookie) {
                          complete(s"Welcome ${user.userName}.")
                        }
                      }
                    case Failure(e) =>
                      complete(InternalServerError, s"Could not generate token: $e")
                  }
                case Failure(e) =>
                  complete(InternalServerError, s"Could not generate token: $e")
              }
            }
          }
        } ~ path("check") {
          authenticate(cookieAuthenticator) { token =>
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
        } ~ path("logout") {
          authenticate(cookieAuthenticator) { token =>
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
        } ~ pathPrefix("posts") {
          authenticate(cookieAuthenticator) { token =>
            path(IntNumber) {
              id =>
                post {
                  entity(as[PostForm]) {
                    postForm =>
                      if (postForm.csrf == token.csrf) {
                        val post = Post(id = postForm.id, title = postForm.title, summary = postForm.summary)
                        ctx => upsertPost(ctx, token, id, post)
                      } else {
                        complete(Forbidden, "CSRF Prevented")
                      }
                  }
                }
            }
          }
        }
      }

  def loginRejectionHandler: RejectionHandler = RejectionHandler {
    case MissingCookieRejection(providedName) :: _ if providedName == cookieName =>
      complete(html.adminlogin.render())
    case ValidationRejection(reason, _) :: _ if reason == "No token." || reason == "Invalid token." =>
      deleteCookie(cookieName) {
        complete(html.adminlogin.render())
      }
  } orElse RejectionHandler.Default

  def tokenRefresh(token: Token): Token = {
    val now = DateTime.now
    if (now + 2.days > token.expiry) {
      TokenGenerator.renewToken(token)
    } else {
      token
    }
  }

  def upsertPost(reqContext: RequestContext, token: Token, id: Int, post: Post): Unit = {
    log.info(s"[$apiScope] Upserting post with id $id: $post.")
    handleWithDb(reqContext) {
      db =>
        val postsWorker = context.actorOf(PostsWorker.props(reqContext, db))
        postsWorker ! UpsertPost(id, post)
    }
  }

}
