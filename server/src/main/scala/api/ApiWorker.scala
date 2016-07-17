package api

import akka.actor.{Actor, ActorLogging, PoisonPill}
import api.jsonserializer.ApiFormatter
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext


abstract class ApiWorker(ctx: RequestContext) extends Actor with Json4sSupport with ActorLogging with ApiFormatter {
  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    ctx.complete(status, obj)
    self ! PoisonPill
  }

  def ok[T <: AnyRef](obj: T) = {
    complete(OK, obj)
  }

  def internalError[T <: AnyRef](obj: T) = {
    complete(InternalServerError, obj)
  }

  def badRequest[T <: AnyRef](obj: T) = {
    complete(BadRequest, obj)
  }

  def notFound[T <: AnyRef](obj: T) = {
    complete(NotFound, obj)
  }
}
