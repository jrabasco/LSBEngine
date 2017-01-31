package me.sblog.api.admin.security

import java.security.SecureRandom

import com.github.nscala_time.time.Imports._
import me.sblog.database.model.Token

object TokenGenerator {
  def generateToken(userName: String): Token = {
    val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
    random.nextBytes(new Array[Byte](32))
    val tokenIdBytes = new Array[Byte](64)
    val csrfBytes = new Array[Byte](64)
    random.nextBytes(tokenIdBytes)
    random.nextBytes(csrfBytes)
    Token(base64Encode(tokenIdBytes), userName, base64Encode(csrfBytes), DateTime.now + 2.weeks)
  }

  def renewToken(token: Token): Token = {
    token.copy(expiry = DateTime.now + 2.weeks)
  }
}
