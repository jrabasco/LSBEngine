package me.lsbengine.bin

import me.lsbengine.api.admin.security.{PasswordHasher, _}
import me.lsbengine.database.model.MongoFormats.userFormat
import me.lsbengine.database.model.{MongoCollections, User}
import me.lsbengine.server.BlogConfiguration
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteConcern
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONRegex}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UserManager extends App {

  val driver = new MongoDriver
  val mongoHost = BlogConfiguration.mongoDBHost
  val mongodbName = BlogConfiguration.mongoDBName

  val connection: MongoConnection = driver.connection(List(mongoHost))

  if (args.length < 1) {
    printAndExit(s"Needs at least 2 arguments.")
  } else {
    args(0) match {
      case "add" =>
        if (args.length < 3) {
          printAndExit(s"add needs 2 arguments: username and password")
        } else {
          val username = args(1)
          val password = args(2)
          println(s"Username: $username")
          println(s"Password: $password")
          val salt = PasswordHasher.generateSalt()
          val b64Salt = base64Encode(salt)
          val hashedPassword = PasswordHasher.hashPassword(password, salt)
          val b64HashedPassword = base64Encode(hashedPassword)
          val user = User(username, b64HashedPassword, b64Salt)

          connection.database(mongodbName).onComplete {
            case Success(db) =>
              val usersAccessor = new UsersAccessor(db)
             usersAccessor.getUser(username).onComplete {
                case Success(maybeUser) =>
                  maybeUser match {
                    case Some(_) =>
                      println(s"User $username already in database. Replace [y/N] ?")
                      val answer = scala.io.StdIn.readLine()
                      if (answer == "y" || answer == "Y") {
                        usersAccessor.updateUser(user).onComplete {
                          case Success(w) =>
                            if (w.ok) {
                              printAndExit(s"User created.")
                            } else {
                              printAndExit(s"Write result not ok.")
                            }
                          case Failure(e) =>
                            printAndExit(s"Could not create user: $e")
                        }
                      } else {
                        printAndExit("OK.")
                      }
                    case None =>
                      usersAccessor.upsertUser(user).onComplete {
                        case Success(w) =>
                          if (w.ok) {
                            printAndExit(s"User created.")
                          } else {
                            printAndExit(s"Write result not ok.")
                          }
                        case Failure(e) =>
                          printAndExit(s"Could not create user: $e")
                      }
                  }
                case Failure(e) =>
                  printAndExit(s"Error while querying database: $e")
              }
            case Failure(e) =>
              printAndExit(s"Could not connect to database: $e")
          }
        }
      case "remove" =>
        if (args.length < 2) {
          printAndExit(s"remove needs 1 argument: username")
        } else {
          val username = args(1)

          println(s"Removing user $username...")

          connection.database(mongodbName).onComplete {
            case Success(db) =>
              val usersCollection = db[BSONCollection](MongoCollections.usersCollectionName)
              val removeSelector = BSONDocument("userName" -> BSONRegex(s"^$username$$", "i"))
              usersCollection.findAndRemove(removeSelector).onComplete {
                case Success(result) =>
                  result.result match {
                    case Some(user) =>
                      val tokensAccessor = new TokensAccessor(db)
                      tokensAccessor.removeTokenWithUserName(user.userName).onComplete {
                        case Success(_) =>
                          printAndExit(s"Successfully removed $username")
                        case Failure(e) =>
                          printAndExit(s"User was removed but not token: $e")
                      }
                    case None =>
                      printAndExit(s"No user $username found.")
                  }
                case Failure(e) =>
                  printAndExit(s"Could not remove: $e.")
              }
            case Failure(e) =>
              printAndExit(s"Could not connect to database: $e")
          }
        }
      case _ =>
        printAndExit(s"Unsupported operation: ${args(0)}")
    }

  }

  private def printAndExit(message: String): Unit = {
    println(message)
    shutdown()
  }

  private def shutdown(): Unit = {
    connection.close()
    driver.system.terminate()
    driver.close()
  }
}
