package api.jsonserializer

import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats}

trait ApiFormatter {
  implicit lazy val json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all
}
