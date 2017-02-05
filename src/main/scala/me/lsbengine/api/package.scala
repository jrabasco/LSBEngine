package me.lsbengine

import me.lsbengine.database.model.Post

package object api {

  case class ListAction()

  case class FetchPost(id: Int)

  case class ListActionResponse(list: List[Post])

  case class FetchPostResponse(post: Post)

}
