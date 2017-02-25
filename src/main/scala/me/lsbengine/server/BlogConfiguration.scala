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
  val blogOwnerFirstName: String = envOrElse("BLOG_OWNER_FIRST_NAME", "Jeremy")
  val blogOwnerLastName: String = envOrElse("BLOG_OWNER_LAST_NAME", "Rabasco")
  val blogOwner: String = blogOwnerFirstName + " " + blogOwnerLastName
  val blogOwnerPseudo: String = envOrElse("BLOG_OWNER_PSEUDO", "")
  val blogOwnerGender: String = envOrElse("BLOG_OWNER_GENDER", "male")
  val blogMetaDescription: String = envOrElse("BLOG_META_DESCRIPTION", "My name is Jeremy Rabasco. I am a Computer Science major and I currently work at <JOB_HERE>.")
  val contactAddress: String = envOrElse("CONTACT_ADDRESS", "rabasco.jeremy@gmail.com")
  val headerTitle: String = envOrElse("HEADER_TITLE", "LSBEngine")
  val siteUrl: String = envOrElse("SITE_UTL", "local.lsbengine.me")
}
