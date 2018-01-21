package me.lsbengine.server

import scala.util.Properties._

object BlogConfiguration {
  val adminPort: Int = envOrElse("ADMIN_PORT", "9090").toInt
  val appContext: String = envOrElse("APP_CONTEXT", "DEV")
  val blogMetaDescription: String = envOrElse("BLOG_META_DESCRIPTION", "My name is Jeremy Rabasco. I am a Computer Science major and I currently work at <JOB_HERE>.")
  val blogOwnerFirstName: String = envOrElse("BLOG_OWNER_FIRST_NAME", "Jeremy")
  val blogOwnerLastName: String = envOrElse("BLOG_OWNER_LAST_NAME", "Rabasco")
  val blogOwner: String = blogOwnerFirstName + " " + blogOwnerLastName
  val blogOwnerGender: String = envOrElse("BLOG_OWNER_GENDER", "male")
  val blogOwnerPseudo: String = envOrElse("BLOG_OWNER_PSEUDO", "")
  val contactAddress: String = envOrElse("CONTACT_ADDRESS", "rabasco.jeremy@gmail.com")
  val copyright: String = envOrElse("COPYRIGHT", "Copyright &copy; 2017")
  val defaultPostsPerPage: Int = envOrElse("DEFAULT_POSTS_PER_PAGE", "5").toInt
  val disclaimer: String = envOrElse("DISCLAIMER", "My opinions do not necessarily represent those of my employer.")
  val hashIterations: Int = envOrElse("HASH_ITERATIONS", "300000").toInt
  val headerTitle: String = envOrElse("HEADER_TITLE", "LSBEngine")
  val hostName: String = envOrElse("SERVER_HOST", "localhost")
  val mongoDBHost: String = envOrElse("MONGO_HOST", "localhost")
  val mongoDBName: String = envOrElse("MONGO_NAME", "lsbengine")
  val mongoDBPort: Int = envOrElse("MONGO_CUST_PORT", "27017").toInt
  val publicPort: Int = envOrElse("PUBLIC_PORT", "8080").toInt
  val repositoryLink: String = envOrElse("REPOSITORY_LINK", "https://github.com/jrabasco/LSBEngine")
  val siteUrl: String = envOrElse("SITE_UTL", "local.lsbengine.me")
}
