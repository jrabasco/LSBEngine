package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair, `Access-Control-Allow-Credentials`}
import akka.http.scaladsl.server._
import com.github.nscala_time.time.Imports._
import me.lsbengine.api.PostsAccessor
import me.lsbengine.api.admin.AdminPostsAccessor
import me.lsbengine.api.admin.security.{cookieName, _}
import me.lsbengine.api.model.{PostCreationResponse, TokenResponse}
import me.lsbengine.database.model.{Post, Token}
import me.lsbengine.errors
import me.lsbengine.pages.admin
import reactivemongo.api.{DefaultDB, MongoConnection}

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
              ctx => editForm(ctx, token, id)
            }
        }
      }
    } ~ pathPrefix("addform") {
      cookieAuthenticator { token =>
        get {
          ctx => addForm(ctx, token)
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
            optionalHeaderValueByName(csrfHeaderName) { csrf =>
              if (csrf.getOrElse("Invalid") == token.csrf) {
                pathEndOrSingleSlash {
                  post {
                    entity(as[Post]) {
                      postData =>
                        ctx => createPost(ctx, postData)
                    }
                  }
                } ~ path(IntNumber) {
                  id =>
                    put {
                      entity(as[Post]) {
                        postData =>
                          ctx => updatePost(ctx, id, postData)
                      }
                    } ~ delete {
                      ctx => deletePost(ctx, id)
                    }
                }
              } else {
                complete(Forbidden, "CSRF Prevented")
              }
            }
          }
        } ~ pathPrefix("trash") {
          cookieAuthenticator { token =>
            get {
              ctx => downloadTrash(ctx)
            } ~ optionalHeaderValueByName(csrfHeaderName) { csrf =>
              if (csrf.getOrElse("Invalid") == token.csrf) {
                delete {
                  ctx => purgeTrash(ctx)
                }
              } else {
                complete(Forbidden, "CSRF Prevented")
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

  private def updatePost(requestContext: RequestContext, id: Int, post: Post): Future[RouteResult] = {
    log.info(s"[$apiScope] Updating post with id $id: $post.")
    handleWithDb(requestContext) {
      db =>
        //to be able to call the upsert
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.updatePost(id, post).flatMap {
          updateWriteResult =>
            if (updateWriteResult.ok && updateWriteResult.n > 0) {
              requestContext.complete("Updated.")
            } else if (!updateWriteResult.ok){
              requestContext.complete(InternalServerError, "Write result is not ok.")
            } else {
              requestContext.complete(NotFound, s"Post $id not found.")
            }
        }.recoverWith {
          case e => requestContext.complete(InternalServerError, s"$e")
        }
    }
  }

  private def createPost(requestContext: RequestContext, post: Post): Future[RouteResult] = {
    log.info(s"[$apiScope] Creating post : $post")
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.createPost(post).flatMap {
          case Some(createdPost) =>
            requestContext.complete(PostCreationResponse(createdPost.id))
          case None =>
            requestContext.complete(InternalServerError, "Could not create post.")
        }.recoverWith {
          case e => requestContext.complete(InternalServerError, s"$e")
        }
    }
  }

  private def deletePost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    log.info(s"[$apiScope] Deleting post with id $id: $post.")
    handleWithDb(requestContext) {
      db =>
        //to be able to call the upsert
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.deletePost(id).flatMap {
          writeResult =>
            if (writeResult.ok) {
              requestContext.complete("Deleted.")
            } else {
              requestContext.complete(InternalServerError, "Write result is not ok.")
            }
        }.recoverWith {
          case e => requestContext.complete(InternalServerError, s"$e")
        }
    }
  }

  private def downloadTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Downloading trash.")
    handleWithDb(requestContext) {
      db =>
        val trashAccessor = new AdminPostsAccessor(db)
        trashAccessor.getTrash.flatMap {
          posts =>
            requestContext.complete(posts)
        }
    }
  }

  private def purgeTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Purging trash.")
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)
        postsAccessor.purgeTrash.flatMap {
          _ => requestContext.complete("Done.")
        }
    }
  }

  private def addForm(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    requestContext.complete(admin.html.addedit.render(token, Post(-1, "", "", "", DateTime.now + 10.years), add = true))
  }

  private def editForm(requestContext: RequestContext, token: Token, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) {
      db =>
        val postsAccessor = new AdminPostsAccessor(db)

        postsAccessor.getPost(id).flatMap {
          case Some(post) =>
            requestContext.complete(admin.html.addedit.render(token, post, add = false))
          case None =>
            requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id does not exist."))
        }.recoverWith {
          case e =>
            requestContext.complete(InternalServerError, errors.html.internalerror.render(s"Failed to retrieve post $id : $e"))
        }
    }
  }

}
