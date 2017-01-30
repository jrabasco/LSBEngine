package me.sblog.api.admin

import java.util.Base64

package object security {

  val cookieName = "admin-access"

  def base64Encode(bytes: Array[Byte]): String = {
    new String(Base64.getEncoder.encode(bytes))
  }

  def base64Decode(string: String): Array[Byte] = {
    Base64.getDecoder.decode(string)
  }
}
