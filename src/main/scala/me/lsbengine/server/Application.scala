package me.lsbengine.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.io.IO
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import spray.can.Http
import sun.misc.{Signal, SignalHandler}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object Application extends App {
  implicit val system = ActorSystem("server")
  implicit val context = ExecutionContext.Implicits.global
  val conf = ConfigFactory.load()

  val driver = new MongoDriver
  val mongoHost = ApplicationConfiguration.mongoDBHost
  val mongodbName = ApplicationConfiguration.mongoDBName
  val connection: MongoConnection = driver.connection(List(mongoHost))
  val hostName = ApplicationConfiguration.hostName
  val frontendPort = ApplicationConfiguration.publicPort
  val adminPort = ApplicationConfiguration.adminPort

  val frontendService = system.actorOf(PublicService.props(connection, mongodbName), "public-service")
  val adminService = system.actorOf(AdminService.props(connection, mongodbName), "admin-service")

  DateTimeZone.setDefault(DateTimeZone.forID("UTC"))

  IO(Http) ! Http.Bind(frontendService, interface = hostName, port = frontendPort)
  IO(Http) ! Http.Bind(adminService, interface = hostName, port = adminPort)

  Signal.handle(new Signal("INT"), new SignalHandler() {
    def handle(sig: Signal) {
      shutdown()
    }
  })

  Signal.handle(new Signal("TERM"), new SignalHandler() {
    def handle(sig: Signal) {
      shutdown()
    }
  })

  private def shutdown(): Unit = {
    println("System is shutting down...")
    IO(Http) ! Http.Unbind(Duration(10, TimeUnit.SECONDS))
    connection.close()
    driver.system.terminate()
    driver.close()
    system.terminate()
  }
}
