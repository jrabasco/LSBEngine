package server

import scala.util.Properties._

object ApplicationConfiguration {
  val hostName = envOrElse("SERVER_HOST", "localhost")
  val frontendPort = envOrElse("FRONTEND_PORT", "8080").toInt
  val adminPort = envOrElse("ADMIN_PORT", "9090").toInt
  val repositoryLink = "https://github.com/Rabyss/MyScalaBlog"
  val mongoDBHost = envOrElse("MYSCALABLOG_MONGO_HOST", "localhost")
  val mongoDBName = envOrElse("MYSCALABLOG_MONGO_NAME", "myscalablog")
  val mongoDBPort = envOrElse("MYSCALABLOG_MONGO_PORT", "27017").toInt
}
