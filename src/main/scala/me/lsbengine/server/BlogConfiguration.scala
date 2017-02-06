package me.lsbengine.server

import scala.util.Properties._

object BlogConfiguration {
  val appContext: String = envOrElse("APP_CONTEXT", "DEV")
  val hostName: String = envOrElse("SERVER_HOST", "localhost")
  val publicPort: Int = envOrElse("PUBLIC_PORT", "8080").toInt
  val adminPort: Int = envOrElse("ADMIN_PORT", "9090").toInt
  val repositoryLink: String = envOrElse("REPOSITORY_LINK", "https://github.com/jrabasco/LSBEngine")
  val mongoDBHost: String = envOrElse("MONGO_HOST", "localhost")
  val mongoDBPort: Int = envOrElse("MONGO_PORT", "27017").toInt
  val mongoDBName: String = envOrElse("MONGO_NAME", "lsbengine")
  val hashIterations: Int = envOrElse("HASH_ITERATIONS", "300000").toInt
  val blogOwner: String = envOrElse("BLOG_OWNER", "J&eacute;r&eacute;my Rabasco")
  val contactAddress: String = envOrElse("CONTACT_ADDRESS", "rabasco.jeremy@gmail.com")
  val headerTitle: String = envOrElse("HEADER_TITLE", "LSBEngine")
}
