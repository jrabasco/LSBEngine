package me.lsbengine.api.admin.security

import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, User}
import me.lsbengine.database.model.MongoFormats.userFormat
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONDocument, BSONRegex}

import scala.concurrent.Future

class UsersAccessor(database: DefaultDB) extends DatabaseAccessor[User](database, MongoCollections.usersCollectionName) {

  def selectorForUsername(userName: String) = BSONDocument("userName" -> BSONRegex(s"^$userName$$", "i"))

  def getUser(userName: String): Future[Option[User]] = {
    super.getItem(selectorForUsername(userName))
  }

  def updateUser(user: User): Future[UpdateWriteResult] = {
    super.updateItem(selectorForUsername(user.userName), user)
  }
}
