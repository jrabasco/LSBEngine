package me.lsbengine.api.admin.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import me.lsbengine.server.BlogConfiguration

object PasswordHasher {
  private val keyLength = 512
  // Needs to be tested on the final hardware
  private val defaultIterations = BlogConfiguration.hashIterations

  def hashPassword(password: Array[Char], salt: Array[Byte], iterations: Int): Array[Byte] = {
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val spec = new PBEKeySpec(password, salt, iterations, keyLength)
    val key = skf.generateSecret(spec)
    key.getEncoded
  }

  def generateSalt(): Array[Byte] = {
    val random = new SecureRandom()
    random.nextBytes(new Array[Byte](32))
    val salt = new Array[Byte](64)
    random.nextBytes(salt)
    salt
  }

  def hashPassword(password: String, salt: Array[Byte], iterations: Int = defaultIterations): Array[Byte] = {
    hashPassword(password.toCharArray, salt, iterations)
  }
}
