package me.sblog.server

import scala.util.Properties._

object ApplicationConfiguration {
  val hostName: String = envOrElse("SERVER_HOST", "localhost")
  val publicPort: Int = envOrElse("PUBLIC_PORT", "8080").toInt
  val adminPort: Int = envOrElse("ADMIN_PORT", "9090").toInt
  val repositoryLink = "https://github.com/jrabasco/SBlog"
  val mongoDBHost: String = envOrElse("MYSCALABLOG_MONGO_HOST", "localhost")
  val mongoDBName: String = envOrElse("MYSCALABLOG_MONGO_NAME", "sblog")
  val mongoDBPort: Int = envOrElse("MYSCALABLOG_MONGO_PORT", "27017").toInt
}
