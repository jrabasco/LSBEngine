package me.lsbengine.api.admin

import reactivemongo.api.commands.UpdateWriteResult

import scala.concurrent.Future

trait SimpleResourceAccessor[T] {

  def getResource: Future[T]

  def setResource(t: T): Future[UpdateWriteResult]
}
