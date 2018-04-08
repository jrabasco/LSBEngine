package me.lsbengine.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}

import me.lsbengine.api.{PostsAccessor, ProjectsAccessor}
import me.lsbengine.api.admin.{AboutMeAccessor, CategoriesAccessor}
import me.lsbengine.api.public.{PublicPostsAccessor, PublicProjectsAccessor}
import me.lsbengine.database.model.{AboutMe, Categories}
import me.lsbengine.errors
import me.lsbengine.pages.public.html
import me.lsbengine.rss.xml
import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class PublicService(dbConnection: MongoConnection, dbName: String, log: LoggingAdapter)
  extends ServerService(dbConnection, dbName, log) {

  override val ownRoutes: Route =
    handleRejections(rejectionHandler) {
      pathSingleSlash {
        parameter("category"?, "page".as[Int]?, "posts_per_page".as[Int]?) {
          (cat, page, postsPerPage) =>
            ctx => index(ctx, cat, page, postsPerPage)
        }
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
      } ~ pathPrefix("feed") {
        path("posts") {
          ctx => postsRssFeed(ctx)
        } ~ path("projects") {
          ctx => projectsRssFeed(ctx)
        }
      }
    }

  override val apiScope: String = "public"

  private def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder().handleNotFound { complete((NotFound, html.notfound.render())) }.result()

  private def index(requestContext: RequestContext, cat: Option[String], page: Option[Int], postsPerPage: Option[Int]): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val postsAccessor = getPostsAccessor(db)
      val aboutMeAccessor = new AboutMeAccessor(db)
      val categoriesAccessor = new CategoriesAccessor(db)

      postsAccessor.listPosts(cat, page, postsPerPage).flatMap {
        case (list, lastPage) =>
          aboutMeAccessor.getResource.flatMap { aboutMe =>
            categoriesAccessor.getResource.flatMap { cats =>
              requestContext.complete(html.index.render(list, conf, cats, aboutMe, cat, page.getOrElse(1), postsPerPage.getOrElse(BlogConfiguration.defaultPostsPerPage), lastPage))
            }
          }
      }.recoverWith {
        case _ =>
          requestContext.complete(html.index.render(List(),
              conf, Categories(titles=List()), AboutMe(None, None), cat,
              page.getOrElse(1), postsPerPage.getOrElse(BlogConfiguration.defaultPostsPerPage), 1))
      }
    }
  }

  private def postsRssFeed(requestContext: RequestContext): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val postsAccessor = getPostsAccessor(db)
      
      postsAccessor.listPosts(None).flatMap {
        case (list, _ ) =>
          requestContext.complete(xml.posts.render(list))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  private def individualPost(requestContext: RequestContext, id: Int): Future[RouteResult] = {
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

  private def about(requestContext: RequestContext): Future[RouteResult] = {
    handleWithNavBarConf(requestContext) { (db, conf) =>
      val aboutMeAccessor = new AboutMeAccessor(db)
      aboutMeAccessor.getResource.flatMap { aboutMe =>
        requestContext.complete(html.about.render(conf, aboutMe))
      }
    }.recoverWith {
      case _ => requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
    }
  }

  private def projects(requestContext: RequestContext): Future[RouteResult] = {
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

  private def projectsRssFeed(requestContext: RequestContext): Future[RouteResult] = {
    handleWithDb(requestContext) { db =>
      val projectsAccessor = getProjectsAccessor(db)
      
      projectsAccessor.listProjects.flatMap { list =>
          requestContext.complete(xml.projects.render(list))
      }.recoverWith {
        case _ =>
          requestContext.complete(InternalServerError, errors.html.internalerror(s"Database access failed."))
      }
    }
  }

  private def individualProject(requestContext: RequestContext, id: Int): Future[RouteResult] = {
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
