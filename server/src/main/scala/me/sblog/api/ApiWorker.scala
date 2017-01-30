package me.sblog.api

import akka.actor.{Actor, ActorLogging, PoisonPill}
import me.sblog.api.jsonserializer.ApiFormatter
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext


abstract class ApiWorker(ctx: RequestContext) extends Actor with Json4sSupport with ActorLogging with ApiFormatter {
  def complete[T <: AnyRef](status: StatusCode, obj: T): Unit = {
    ctx.complete(status, obj)
    self ! PoisonPill
  }

  def ok[T <: AnyRef](obj: T): Unit = {
    complete(OK, obj)
  }

  def internalError[T <: AnyRef](obj: T): Unit = {
    complete(InternalServerError, obj)
  }

  def badRequest[T <: AnyRef](obj: T): Unit = {
    complete(BadRequest, obj)
  }

  def notFound[T <: AnyRef](obj: T): Unit = {
    complete(NotFound, obj)
  }
}
