package me.lsbengine.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import reactivemongo.api.{MongoConnection, MongoDriver}
import sun.misc.Signal

import scala.concurrent.ExecutionContext

object Blog extends App {
  implicit val system = ActorSystem("server")
  implicit val context = ExecutionContext.Implicits.global
  implicit val materializer = ActorMaterializer()
  val conf = ConfigFactory.load()

  val driver = new MongoDriver
  val mongoHost = BlogConfiguration.mongoDBHost
  val mongodbName = BlogConfiguration.mongoDBName
  val connection: MongoConnection = driver.connection(List(mongoHost))
  val hostName = BlogConfiguration.hostName
  val publicPort = BlogConfiguration.publicPort
  val adminPort = BlogConfiguration.adminPort

  val publicService = new PublicService(connection, mongodbName, Http().system.log)
  val adminService = new AdminService(connection, mongodbName, Http().system.log)

  DateTimeZone.setDefault(DateTimeZone.forID("UTC"))

  Http().bindAndHandle(publicService.routes , hostName, publicPort)
  Http().bindAndHandle(adminService.routes, hostName, adminPort)

  Signal.handle(new Signal("INT"), (_: Signal) => {
    shutdown()
  })

  Signal.handle(new Signal("TERM"), (_: Signal) => {
    shutdown()
  })

  private def shutdown(): Unit = {
    println("System is shutting down...")
    Http().shutdownAllConnectionPools()
    connection.close()
    driver.system.terminate()
    driver.close()
    system.terminate()
  }
}
