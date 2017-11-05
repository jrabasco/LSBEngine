package me.lsbengine.api

import me.lsbengine.database.model._

package object model {

  case class ListPostsResponse(list: List[Post])

  case class ListProjectsResponse(list: List[Project])

  case class FetchPostResponse(post: Post)

  case class FetchProjectResponse(project: Project)

  case class TokenResponse(message: String)

  case class PostCreationResponse(id: Int)

  case class ProjectCreationResponse(id: Int)

}
