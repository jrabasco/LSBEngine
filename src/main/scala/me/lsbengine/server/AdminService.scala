package me.lsbengine.server

import akka.actor.{ActorRef, Props}
import me.lsbengine.api.admin.{AdminPostsAccessor, AdminPostsWorker}
import me.lsbengine.api.admin.AdminPostsWorker.UpsertPost
import me.lsbengine.api.admin.security.{cookieName, _}
import me.lsbengine.database.model.{Post, PostForm, Token}
import reactivemongo.api.{DefaultDB, MongoConnection}
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
          token =>
            ctx => index(ctx, token)
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
                          TokenGenerator.renewToken(oldToken)
                        case None =>
                          TokenGenerator.generateToken(user.userName)
                      }
                      tokensAccessor.storeToken(newToken)
                      val cookie = generateCookie(newToken)
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
                        ctx => upsertPost(ctx, token, id, postForm.post)
                      } else {
                        complete(Forbidden, "CSRF Prevented")
                      }
                  }
                }
            }
          }
        }
      }

  private def loginRejectionHandler: RejectionHandler = RejectionHandler {
    case MissingCookieRejection(providedName) :: _ if providedName == cookieName =>
      complete(html.adminlogin.render())
    case ValidationRejection(reason, _) :: _ if reason == "No token." || reason == "Invalid token." =>
      deleteCookie(cookieName) {
        complete(html.adminlogin.render())
      }
  } orElse RejectionHandler.Default

  private def upsertPost(reqContext: RequestContext, token: Token, id: Int, post: Post): Unit = {
    log.info(s"[$apiScope] Upserting post with id $id: $post.")
    handleWithDb(reqContext) {
      db =>
        val postsWorker = getPostsWorker(reqContext, db)
        postsWorker ! UpsertPost(id, post)
    }
  }

  private def generateCookie(newToken: Token): HttpCookie = {
    val secure = BlogConfiguration.appContext == "PROD"
    val expires = Some(spray.http.DateTime(newToken.expiry.getMillis))
    // 2 weeks
    val maxAge = Some(2L * 7 * 24 * 60 * 60 * 1000)
    HttpCookie(cookieName,
      content = newToken.tokenId,
      expires = expires,
      maxAge = maxAge,
      path = Some("/"),
      secure = secure,
      httpOnly = true)
  }

  override def getPostsWorker(requestContext: RequestContext, database: DefaultDB): ActorRef = {
    context.actorOf(AdminPostsWorker.props(requestContext, database))
  }

  def index(reqContext: RequestContext, token: Token): Unit = {
    handleWithDb(reqContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.listPosts.onComplete {
          case Success(list) =>
            reqContext.complete(html.adminhome.render(token, list))
          case Failure(_) =>
            reqContext.complete(html.adminhome.render(token, List()))
        }
    }
  }

}
