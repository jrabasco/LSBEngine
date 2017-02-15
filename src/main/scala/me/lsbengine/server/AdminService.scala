package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair, `Access-Control-Allow-Credentials`}
import akka.http.scaladsl.server._
import me.lsbengine.api.PostsAccessor
import me.lsbengine.api.admin.AdminPostsAccessor
import me.lsbengine.api.admin.security.{cookieName, _}
import me.lsbengine.api.model.{PostForm, TokenResponse}
import me.lsbengine.database.model.{Post, Token}
import me.lsbengine.errors
import me.lsbengine.pages.admin
import reactivemongo.api.{DefaultDB, MongoConnection}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class AdminService(val dbConnection: MongoConnection, val dbName: String, val log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log)
    with CredentialsAuthenticator
    with CookiesAuthenticator {

  override val apiScope: String = "admin"

  override val commonRoutes: Route =
    cookieAuthenticator { _ =>
      super.commonRoutes
    }

  override val ownRoutes: Route =
    pathSingleSlash {
      handleRejections(loginRejectionHandler) {
        cookieAuthenticator {
          token =>
            ctx => index(ctx, token)
        }
      }
    } ~ pathPrefix("editform") {
      cookieAuthenticator { token =>
        path(IntNumber) {
          id =>
            get {
              ctx => edit(ctx, token, id)
            }
        }
      }
    } ~ pathPrefix("addform") {
      cookieAuthenticator { token =>
        get {
          ctx => edit(ctx, token, -1)
        }
      }
    } ~
      pathPrefix("api") {
        path("token") {
          credentialsAuthenticator { user =>
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
                          complete(TokenResponse(s"Welcome ${user.userName}."))
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
          cookieAuthenticator { token =>
            log.info(s"[$apiScope] Checking token for user ${token.userName}.")
            get {
              complete(s"User ${token.userName} is logged in.")
            }
          }
        } ~ path("logout") {
          cookieAuthenticator { token =>
            log.info(s"[$apiScope] Logging out user ${token.userName}.")
            onSuccess(dbConnection.database(dbName)) {
              db =>
                val tokensAccessor = new TokensAccessor(db)
                onSuccess(tokensAccessor.removeToken(token.tokenId)) {
                  res =>
                    if (res.ok) {
                      deleteCookie(cookieName) {
                        complete("Successfully logged out.")
                      }
                    } else {
                      complete(InternalServerError, s"Something wrong happened.")
                    }
                }
            }
          }
        } ~ pathPrefix("posts") {
          cookieAuthenticator { token =>
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

  private def loginRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder().handle {
      case MissingCookieRejection(providedName) if providedName == cookieName =>
        complete(admin.html.login.render())
      case ValidationRejection(reason, _) if reason == "No token." || reason == "Invalid token." =>
        deleteCookie(cookieName) {
          complete(admin.html.login.render())
        }
    }.result().withFallback(RejectionHandler.default)


  private def generateCookie(newToken: Token): HttpCookie = {
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

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new AdminPostsAccessor(database)
  }

  def index(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.listPosts.flatMap {
          list =>
            requestContext.complete(admin.html.index.render(token, list))
        }.recoverWith {
          case _ =>
            requestContext.complete(admin.html.index.render(token, List()))
        }
    }
  }

  private def upsertPost(requestContext: RequestContext, token: Token, id: Int, post: Post): Future[RouteResult] = {
    log.info(s"[$apiScope] Upserting post with id $id: $post.")
    handleWithDb(requestContext) {
      db =>
        //to be able to call the upsert
        val postsAccessor = new AdminPostsAccessor(db)
        val selector = BSONDocument("id" -> id)
        postsAccessor.upsertPost(selector, post).flatMap {
          updateWriteResult =>
            if (updateWriteResult.ok) {
              requestContext.complete("Updated.")
            } else {
              requestContext.complete(InternalServerError, "Write result is not ok.")
            }
        }.recoverWith {
          case e => requestContext.complete(InternalServerError, s"$e")
        }
    }
  }

  def edit(requestContext: RequestContext, token: Token, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)
        val futureMaybePost =
          if (id >= 0) {
            postsAccessor.getPost(id)
          } else {
            postsAccessor.getNewEmptyPost
          }
        futureMaybePost.flatMap {
          case Some(post) =>
            requestContext.complete(admin.html.edit.render(token, post))
          case None =>
            requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id does not exist."))
        }.recoverWith {
          case e =>
            requestContext.complete(InternalServerError, errors.html.internalerror.render(s"Failed to retrieve post $id : $e"))
        }
    }
  }

}
