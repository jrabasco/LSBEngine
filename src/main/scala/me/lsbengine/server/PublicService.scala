package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import me.lsbengine.api.{PostsAccessor, ProjectsAccessor}
import me.lsbengine.api.admin.AboutMeAccessor
import me.lsbengine.api.public.{PublicPostsAccessor, PublicProjectsAccessor}
import me.lsbengine.database.model.AboutMe
import me.lsbengine.errors
import me.lsbengine.pages.public.html
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublicService(dbConnection: MongoConnection, dbName: String, log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log) {

  override val ownRoutes: Route =
    pathSingleSlash {
      ctx => index(ctx)
    } ~ pathPrefix("posts") {
      path(IntNumber) { id =>
        ctx => individualPost(ctx, id)
      }
    } ~ pathPrefix("about") {
      ctx => about(ctx)
    } ~ pathPrefix("projects") {
      pathEndOrSingleSlash {
        ctx => projects(ctx)
      } ~ path(IntNumber) { id =>
        ctx => individualProject(ctx, id)
      }
    }

  override val apiScope: String = "public"

  def index(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val postsAccessor = getPostsAccessor(db)
      val aboutMeAccessor = new AboutMeAccessor(db)

      postsAccessor.listPosts.flatMap { list =>
        aboutMeAccessor.getResource.flatMap { aboutMe =>
          requestContext.complete(html.index.render(list, conf, aboutMe))
        }
      }.recoverWith {
        case _ =>
          requestContext.complete(html.index.render(List(), conf, AboutMe(None, None)))
      }
    }
  }

  def individualPost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val postsAccessor = getPostsAccessor(db)
      postsAccessor.getPost(id).flatMap {
        case Some(post) =>
          requestContext.complete(html.post.render(post, conf))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Post $id not found."))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  def about(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val aboutMeAccessor = new AboutMeAccessor(db)
      aboutMeAccessor.getResource.flatMap { aboutMe =>
        requestContext.complete(html.about.render(conf, aboutMe))
      }
    }.recoverWith {
      case _ => requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
    }
  }

  def projects(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val projectsAccessor = getProjectsAccessor(db)

      projectsAccessor.listProjects.flatMap { list =>
        requestContext.complete(html.projects.render(list, conf))
      }.recoverWith {
        case _ =>
          requestContext.complete(html.projects.render(List(), conf))
      }
    }
  }

  def individualProject(requestContext: RequestContext, id: Int): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val projectsAccessor = getProjectsAccessor(db)
      projectsAccessor.getProject(id).flatMap {
        case Some(project) =>
          requestContext.complete(html.project.render(project, conf))
        case None =>
          requestContext.complete(NotFound, errors.html.notfound.render(s"Project $id not found."))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  override def getPostsAccessor(database: DefaultDB): PostsAccessor = {
    new PublicPostsAccessor(database)
  }

  override def getProjectsAccessor(database: DefaultDB): ProjectsAccessor = {
    new PublicProjectsAccessor(database)
  }

}
