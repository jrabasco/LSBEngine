package me.lsbengine

import me.lsbengine.database.model.Post

import scala.concurrent.Future

package object api {

  trait PostsAccessor {
    def getPost(id: Int): Future[Option[Post]]

    def listPosts: Future[List[Post]]
  }

}
