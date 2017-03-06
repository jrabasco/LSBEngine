package me.lsbengine.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import me.lsbengine.api.admin.security.NewCredentials
import me.lsbengine.api.model.{FetchPostResponse, ListActionResponse, PostCreationResponse, TokenResponse}
import me.lsbengine.database.model.{AboutMe, HtmlMarkdownContent, NavBarConf, Post}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.json._

import scala.util.Try

trait JSONSupport extends SprayJsonSupport with DefaultJsonProtocol with CollectionFormats {

  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

    val formatter: DateTimeFormatter = ISODateTimeFormat.dateTime

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => Try(formatter.parseDateTime(s)).fold(_ => error(s), identity)
      case _ =>
        error(json.toString())
    }

    override def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(s"$v is not a valid date. Dates must be in ISO formatForForm : $example")
    }
  }

  //USED ONLY FOR BUILDINFO OTHERWISE STRANGE BEHAVIOUR
  implicit object AnyFormat extends JsonFormat[Any] {
    override def write(obj: Any): JsValue = JsString(s"$obj")

    override def read(json: JsValue): Any = json match {
      case JsString(s) => s
      case _ =>
        deserializationError(s"Expected a string, got $json")
    }
  }

  implicit val htmlMarkdownContentFormat: RootJsonFormat[HtmlMarkdownContent] = jsonFormat2(HtmlMarkdownContent)

  implicit val postFormat: RootJsonFormat[Post] = jsonFormat5(Post)
  implicit val aboutMeFormat: RootJsonFormat[AboutMe] = jsonFormat2(AboutMe)
  implicit val navbarFormat: RootJsonFormat[NavBarConf] = jsonFormat2(NavBarConf)
  implicit val tokenResponseFormat: RootJsonFormat[TokenResponse] = jsonFormat1(TokenResponse)
  implicit val listActionResponseFormat: RootJsonFormat[ListActionResponse] = jsonFormat1(ListActionResponse)
  implicit val fetchPostResponseFormat: RootJsonFormat[FetchPostResponse] = jsonFormat1(FetchPostResponse)
  implicit val postCreationResponseFormat: RootJsonFormat[PostCreationResponse] = jsonFormat1(PostCreationResponse)

  implicit val newCredentialsFormat: RootJsonFormat[NewCredentials] = jsonFormat3(NewCredentials)
}
