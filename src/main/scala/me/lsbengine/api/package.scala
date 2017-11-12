package me.lsbengine

import me.lsbengine.database.model.{Post, Project}

import scala.concurrent.Future

package object api {

  trait PostsAccessor {
    def getPost(id: Int): Future[Option[Post]]

    def listPosts(category: Option[String] = None, pageOpt: Option[Int] = None, postsPerPageOpt: Option[Int] = None): Future[List[Post]]
  }

  trait ProjectsAccessor {
    def getProject(id: Int): Future[Option[Project]]

    def listProjects: Future[List[Project]]
  }

}
