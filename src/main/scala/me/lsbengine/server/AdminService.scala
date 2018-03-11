package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Credentials`
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.github.nscala_time.time.Imports._
import me.lsbengine.api.{PostsAccessor, ProjectsAccessor}
import com.github.nscala_time.time.DurationBuilder
import me.lsbengine.api.admin.security.CredentialsAuthenticator.Credentials
import me.lsbengine.api.admin.security._
import me.lsbengine.api.admin._
import me.lsbengine.api.model.{PostCreationResponse, TokenResponse}
import me.lsbengine.database.model._
import me.lsbengine.errors
import me.lsbengine.pages.admin
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.language.postfixOps
import scala.collection.immutable.Stream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{SECONDS, FiniteDuration}
import scala.util.{Failure, Success}
import java.io.File

class AdminService(val dbConnection: MongoConnection, val dbName: String, val log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log)
    with CredentialsAuthenticator
    with CookiesAuthenticator {

  override val apiScope: String = "admin"

  override val commonRoutes: Route =
    cookieAuthenticator { _ =>
      super.commonRoutes
    }

  val frontendRoutes: Route =
    handleRejections(loginRejectionHandler) {
      pathEndOrSingleSlash {
        cookieAuthenticator { token =>
          get {
            ctx => index(ctx, token)
          }
        }
      } ~ resourceForms("posts", postsIndex, editPostForm, addPostForm) ~
      pathPrefix("posts") {
        cookieAuthenticator { _ =>
          path(IntNumber) { id =>
            ctx => individualPost(ctx, id)
          }
        }
      } ~ resourceForms("projects", projectsIndex, editProjectForm, addProjectForm) ~
      pathPrefix("projects") {
        cookieAuthenticator { _ =>
          path(IntNumber) { id =>
            ctx => individualProject(ctx, id)
          }
        }
      } ~ pathPrefix("password") {
        cookieAuthenticator { token =>
          path("edit") {
            get {
              ctx => passwordForm(ctx, token)
            }
          }
        }
      } ~ pathPrefix("perso") {
        cookieAuthenticator { token =>
          path("edit") {
            get {
              ctx => personalDetailsEdition(ctx, token)
            }
          }
        }
      } ~ pathPrefix("images") {
        cookieAuthenticator { token =>
          pathEndOrSingleSlash {
            get {
              ctx => uploadImagesPage(ctx, token)
            }
          }
        }
      }
    }

  val apiRoutes: Route =
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
      } ~ path("new_password") {
        cookieWithCsrfCheck {
          post {
            entity(as[NewCredentials]) { newCreds =>
              ctx => updatePassword(ctx, newCreds)
            }
          }
        }
      } ~ path("logout") {
        cookieAuthenticator { token =>
          log.info(s"[$apiScope] Logging out user ${token.userName}.")
          onSuccess(dbConnection.database(dbName)) { db =>
            val tokensAccessor = new TokensAccessor(db)
            onSuccess(tokensAccessor.removeToken(token.tokenId)) { res =>
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
        cookieWithCsrfCheck {
          pathEndOrSingleSlash {
            post {
              entity(as[Post]) { postData =>
                ctx => createPost(ctx, postData)
              }
            }
          } ~ path(IntNumber) { id =>
            put {
              entity(as[Post]) { postData =>
                ctx => updatePost(ctx, id, postData)
              }
            } ~ delete {
              ctx => deletePost(ctx, id)
            }
          }
        }
      } ~ pathPrefix("projects") {
        cookieWithCsrfCheck {
          pathEndOrSingleSlash {
            post {
              entity(as[Project]) { projData =>
                ctx => createProject(ctx, projData)
              }
            }
          } ~ path(IntNumber) { id =>
            put {
              entity(as[Project]) { projData =>
                ctx => updateProject(ctx, id, projData)
              }
            } ~ delete {
              ctx => deleteProject(ctx, id)
            }
          }
        }
      } ~ pathPrefix("trash") {
        path("posts") {
          cookieAuthenticator { token =>
            get {
              ctx => downloadPostsTrash(ctx)
            } ~ csrfCheck(token) {
              delete {
                ctx => purgePostsTrash(ctx)
              }
            }
          }
        } ~ path("projects") {
          cookieAuthenticator { token =>
            get {
              ctx => downloadProjectsTrash(ctx)
            } ~ csrfCheck(token) {
              delete {
                ctx => purgeProjectsTrash(ctx)
              }
            }
          }
        }
      } ~ {
        handleSingleResourceUpdate[NavBarConf]("navbar", new NavBarConfAccessor(_))
      } ~ {
        handleSingleResourceUpdate[AboutMe]("perso", new AboutMeAccessor(_))
      } ~ {
        handleSingleResourceUpdate[Categories]("categories", new CategoriesAccessor(_))
      } ~ imagesRoute
    }

  val imagesRoute: Route = toStrictEntity(new FiniteDuration(10, SECONDS)) {
    pathPrefix("images") {
      cookieWithCsrfCheck {
        pathEndOrSingleSlash {
          post {
            formField('fileName) { fileName =>
              uploadedFile("data") { case (metadata, file) =>
                ctx => uploadImage(ctx, metadata, file, fileName)
              }
            }
          }
        } ~ path("""(.+)""".r) { imageName =>
          delete {
            ctx => 
              deleteImage(ctx, imageName)
          }
        }
      }
    }
  }

  override val ownRoutes: Route = frontendRoutes ~ apiRoutes

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new AdminPostsAccessor(database)
  }

  override def getProjectsAccessor(database: DefaultDB): ProjectsAccessor = {
    new AdminProjectsAccessor(database)
  }

  private def resourceForms(resourceName: String,
                            index: (RequestContext, Token, Option[Int], Option[Int]) => Future[RouteResult],
                            edit: (RequestContext, Token, Int) => Future[RouteResult],
                            add: (RequestContext, Token) => Future[RouteResult]): Route = {
    pathPrefix(resourceName) {
      cookieAuthenticator { token =>
        pathEndOrSingleSlash {
          parameters("page".as[Int]?, "items_per_page".as[Int]?) {
            (page, itemsPerPage) =>
              ctx => index(ctx, token, page, itemsPerPage)
          }
        } ~ pathPrefix("edit") {
          path(IntNumber) { id =>
            get {
              ctx => edit(ctx, token, id)
            }
          }
        } ~ pathPrefix("add") {
          get {
            ctx => add(ctx, token)
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
    }.result()

  private def updatePassword(requestContext: RequestContext, newCredentials: NewCredentials): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val oldCreds = Credentials(newCredentials.username, newCredentials.oldPassword)
      getUserAndValidatePassword(db, oldCreds).flatMap {
        case Some(_) =>
          val newSalt = PasswordHasher.generateSalt()
          val hashedPassword = PasswordHasher.hashPassword(newCredentials.newPassword, newSalt)

          val encodedSalt = base64Encode(newSalt)
          val encodedHashedPassword = base64Encode(hashedPassword)

          val newUser = User(newCredentials.username, encodedHashedPassword, encodedSalt)

          val usersAccessor = new UsersAccessor(db)
          usersAccessor.updateUser(newUser).flatMap { updateWriteResult =>
            if (updateWriteResult.ok && updateWriteResult.n > 0) {
              requestContext.complete(OK, s"User ${newUser.userName} updated.")
            } else if (!updateWriteResult.ok) {
              requestContext.complete(InternalServerError, "Write result is not ok.")
            } else {
              // should not happen as we check it before but it does not hurt to check
              requestContext.complete(NotFound, s"User ${newUser.userName} not found.")
            }
          }
        case None =>
          requestContext.complete(Unauthorized, "Invalid username/password combination.")
      }.recoverWith {
        case e => requestContext.complete(InternalServerError, s"$e")
      }
    }
  }

  private def index(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val categoriesAccessor = new CategoriesAccessor(db)
      categoriesAccessor.getResource.flatMap {
        cats => requestContext.complete(OK, admin.html.index.render(token, cats, conf))
      }.recoverWith {
        case _ =>
          requestContext.complete(OK, admin.html.index.render(token, Categories(titles=List()), conf))
      }
    }
  }

  private def uploadImage(requestContext: RequestContext, metadata: FileInfo, file: File, passedFileName: String): Future[RouteResult] = {
    val pattern = """(.*)[.]([^.]*)""".r
    metadata.fileName match {
      case pattern(fn, ext) =>
        handleWithDb(requestContext) { db =>
          val finalName = if (passedFileName.length > 0) passedFileName else fn
          val namePattern = """([a-zA-Z0-9\.\-_]*)""".r
          finalName match {
            case namePattern(nm) =>
              val dest = new File(BlogConfiguration.imagesLocation, finalName + s".$ext")
              val imagesAccessor = new ImagesAccessor(db)
              imagesAccessor.getImage(finalName).flatMap {
                case Some(_) =>
                  requestContext.complete(Conflict, s"Image with name '$finalName' already exists.")
                case None =>
                  val img = Image(finalName, ext, dest.toPath.toString)
                  imagesAccessor.saveImage(img, file).flatMap { updateWriteResult =>
                    if (updateWriteResult.ok) {
                      requestContext.complete("Image saved.")
                    } else {
                      requestContext.complete(InternalServerError, "Write result not ok.")
                    }
                  }.recoverWith {
                    case e => requestContext.complete(InternalServerError, s"$e")
                  }
              }
            case _ =>
              requestContext.complete(BadRequest, "File name must be only comprised of letters, numbers, underscores, hyphens or dots.")
          }
        }
      case _ =>
        requestContext.complete(BadRequest, "File has no extension.")
    }
  }

  private def deleteImage(requestContext: RequestContext, imageName: String): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val imagesAccessor = new ImagesAccessor(db)
      val postsAccessor = new AdminPostsAccessor(db)
      postsAccessor.findPostByThumbnail(imageName).flatMap {
        case Some(post) =>
          requestContext.complete(BadRequest, s"Could not delete image '$imageName' because post ${post.id} (${post.title}) uses it as a thumbnail.")
        case None =>
          imagesAccessor.deleteImage(imageName).flatMap { writeResult =>
            if (writeResult.ok) {
              requestContext.complete("Deleted.")
            } else {
              requestContext.complete(InternalServerError, "Write result is not ok.")
            }
          }
      }.recoverWith {
        case e => requestContext.complete(InternalServerError, s"$e")
      }
    }
  }

  private def postsIndex(requestContext: RequestContext, token: Token, page: Option[Int], postsPerPage: Option[Int]): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminPostsAccessor(db)
      postsAccessor.listPosts(None, page, postsPerPage).flatMap {
        case (list, lastPage) =>
          requestContext.complete(admin.html.postsindex.render(token, list, page.getOrElse(1), postsPerPage.getOrElse(BlogConfiguration.defaultPostsPerPage), lastPage))
      }.recoverWith {
        case _ =>
          requestContext.complete(admin.html.postsindex.render(token, List(), page.getOrElse(1), postsPerPage.getOrElse(BlogConfiguration.defaultPostsPerPage), 1))
      }
    }
  }

  private def individualPost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val postsAccessor = getPostsAccessor(db)
      postsAccessor.getPost(id).flatMap {
        case Some(post) =>
          requestContext.complete(admin.html.post.render(post))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id not found."))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }


  private def projectsIndex(requestContext: RequestContext, token: Token, page: Option[Int], projectsPerPage: Option[Int]): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val projectsAccessor = new AdminProjectsAccessor(db)
      projectsAccessor.listProjects.flatMap {
        list =>
          requestContext.complete(admin.html.projectsindex.render(token, list))
      }.recoverWith {
        case _ =>
          requestContext.complete(admin.html.projectsindex.render(token, List()))
      }
    }
  }

  private def individualProject(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val projectsAccessor = getProjectsAccessor(db)
      projectsAccessor.getProject(id).flatMap {
        case Some(project) =>
          requestContext.complete(admin.html.project.render(project))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Project $id not found."))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  private def editProjectForm(requestContext: RequestContext, token: Token, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val projectsAccessor = new AdminProjectsAccessor(db)

      projectsAccessor.getProject(id).flatMap {
        case Some(project) =>
          requestContext.complete(admin.html.addeditproject.render(token, project, add = false))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id does not exist."))
      }.recoverWith {
        case e =>
          requestContext.complete(InternalServerError, errors.html.internalerror.render(s"Failed to retrieve post $id : $e"))
      }
    }
  }

  private def addProjectForm(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    requestContext.complete(admin.html.addeditproject.render(token, Project(-1, "", "", HtmlMarkdownContent("", ""), DateTime.now + 10.years), add = true))
  }

  private def personalDetailsEdition(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val aboutMeAccessor = new AboutMeAccessor(db)
      aboutMeAccessor.getResource.flatMap { aboutMe =>
        requestContext.complete(admin.html.perso.render(aboutMe, token))
      }
    }.recoverWith {
      case e => requestContext.complete(InternalServerError, errors.html.internalerror(s"$e"))
    }
  }

  private def uploadImagesPage(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val imagesAccessor = new ImagesAccessor(db)
      imagesAccessor.getImages.flatMap { images =>
        requestContext.complete(admin.html.images.render(token, images))
      }
    }.recoverWith {
      case e => requestContext.complete(InternalServerError, errors.html.internalerror(s"$e"))
    }
  }

  private def updateSimpleResource[T](requestContext: RequestContext,
                                      accessorCreator: DefaultDB => SimpleResourceAccessor[T],
                                      t: T): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val simpleResourceAccessor = accessorCreator(db)
      simpleResourceAccessor.setResource(t).flatMap { updateWriteResult =>
        if (updateWriteResult.ok) {
          requestContext.complete(OK, "Updated.")
        } else {
          requestContext.complete(InternalServerError, "Write result not ok.")
        }
      }.recoverWith {
        case e => requestContext.complete(InternalServerError, s"$e")
      }
    }
  }

  private def updatePost(requestContext: RequestContext, id: Int, post: Post): Future[RouteResult] = {
    log.info(s"[$apiScope] Updating post with id $id: $post.")
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminPostsAccessor(db)
      postsAccessor.updatePost(id, post).flatMap { updateWriteResult =>
        if (updateWriteResult.ok && updateWriteResult.n > 0) {
          requestContext.complete("Updated.")
        } else if (!updateWriteResult.ok) {
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
    handleWithDb(requestContext) { db =>
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
    log.info(s"[$apiScope] Deleting post with id $id.")
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminPostsAccessor(db)
      postsAccessor.deletePost(id).flatMap { writeResult =>
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

  private def updateProject(requestContext: RequestContext, id: Int, project: Project): Future[RouteResult] = {
    log.info(s"[$apiScope] Updating project with id $id: $project.")
    handleWithDb(requestContext) { db =>
      val projectsAccessor = new AdminProjectsAccessor(db)
      projectsAccessor.updateProject(id, project).flatMap { updateWriteResult =>
        if (updateWriteResult.ok && updateWriteResult.n > 0) {
          requestContext.complete("Updated.")
        } else if (!updateWriteResult.ok) {
          requestContext.complete(InternalServerError, "Write result is not ok.")
        } else {
          requestContext.complete(NotFound, s"Project $id not found.")
        }
      }.recoverWith {
        case e => requestContext.complete(InternalServerError, s"$e")
      }
    }
  }

  private def createProject(requestContext: RequestContext, project: Project): Future[RouteResult] = {
    log.info(s"[$apiScope] Creating post : $post")
    handleWithDb(requestContext) { db =>
      val projectsAccessor = new AdminProjectsAccessor(db)
      projectsAccessor.createProject(project).flatMap {
        case Some(createdProject) =>
          requestContext.complete(PostCreationResponse(createdProject.id))
        case None =>
          requestContext.complete(InternalServerError, "Could not create project.")
      }.recoverWith {
        case e => requestContext.complete(InternalServerError, s"$e")
      }
    }
  }

  private def deleteProject(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    log.info(s"[$apiScope] Deleting project with id $id.")
    handleWithDb(requestContext) { db =>
      val projectsAccessor = new AdminProjectsAccessor(db)
      projectsAccessor.deleteProject(id).flatMap { writeResult =>
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

  private def handleSingleResourceUpdate[Resource](resourceName: String, accessorCreator: DefaultDB => SimpleResourceAccessor[Resource])
                                                  (implicit unMarshaller: FromRequestUnmarshaller[Resource]): Route = {
    pathPrefix(resourceName) {
      cookieWithCsrfCheck {
        pathEndOrSingleSlash {
          put {
            entity(as[Resource]) { res =>
              ctx => updateSimpleResource(ctx, accessorCreator, res)
            }
          }
        }
      }
    }
  }

  private def downloadPostsTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Downloading posts trash.")
    handleWithDb(requestContext) { db =>
      val trashAccessor = new AdminPostsAccessor(db)
      trashAccessor.getTrash.flatMap { posts =>
        requestContext.complete(posts)
      }
    }
  }

  private def purgePostsTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Purging posts trash.")
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminPostsAccessor(db)
      postsAccessor.purgeTrash.flatMap {
        _ => requestContext.complete("Done.")
      }
    }
  }

  private def downloadProjectsTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Downloading projects trash.")
    handleWithDb(requestContext) { db =>
      val trashAccessor = new AdminProjectsAccessor(db)
      trashAccessor.getTrash.flatMap { posts =>
        requestContext.complete(posts)
      }
    }
  }

  private def purgeProjectsTrash(requestContext: RequestContext): Future[RouteResult] = {
    log.info(s"[$apiScope] Purging projets trash.")
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminProjectsAccessor(db)
      postsAccessor.purgeTrash.flatMap {
        _ => requestContext.complete("Done.")
      }
    }
  }

  private def addPostForm(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val categoriesAccessor = new CategoriesAccessor(db)
      val imagesAccessor = new ImagesAccessor(db)
      categoriesAccessor.getResource.flatMap { categories =>
        imagesAccessor.getImages.flatMap { images =>
          val imageNames = images.map(img => img.name + "." +  img.extension)
          requestContext.complete(admin.html.addeditpost.render(token, Post(-1, "", "", HtmlMarkdownContent("", ""), DateTime.now + 10.years, explicit=false, category=None), categories, add = true, imageNames))
        }
      }
    }
  }

  private def editPostForm(requestContext: RequestContext, token: Token, id: Int): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val postsAccessor = new AdminPostsAccessor(db)
      val categoriesAccessor = new CategoriesAccessor(db)
      val imagesAccessor = new ImagesAccessor(db)

      categoriesAccessor.getResource.flatMap { categories =>
        imagesAccessor.getImages.flatMap { images =>
          val imageNames = images.map(img => img.name + "." +  img.extension)
          postsAccessor.getPost(id).flatMap {
            case Some(post) =>
              requestContext.complete(admin.html.addeditpost.render(token, post, categories, add = false, imageNames))
            case None =>
              requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id does not exist."))
          }
        }
      }.recoverWith {
        case e =>
          requestContext.complete(InternalServerError, errors.html.internalerror.render(s"Failed to retrieve post $id : $e"))
      }
    }
  }

  private def passwordForm(requestContext: RequestContext, token: Token): Future[RouteResult] = {
    requestContext.complete(admin.html.editpassword.render(token))
  }

}
