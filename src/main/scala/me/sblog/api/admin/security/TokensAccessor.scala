package me.sblog.api.admin.security

import me.sblog.database.model.{MongoCollections, Token}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.api.commands.{UpdateWriteResult, WriteConcern, WriteResult}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TokensAccessor(db: DefaultDB) {
  val tokensCollection: BSONCollection = db[BSONCollection](MongoCollections.tokensCollectionName)

  def getTokenWithId(tokenId: String): Future[Option[Token]] = {
    val query = BSONDocument("tokenId" -> tokenId)
    tokensCollection.find(query).one[Token]
  }

  def getTokenWithUserName(userName: String): Future[Option[Token]] = {
    val query = BSONDocument("userName" -> userName)
    tokensCollection.find(query).one[Token]
  }

  def storeToken(token: Token): Future[UpdateWriteResult] = {
    val selector = BSONDocument("userName" -> token.userName)
    tokensCollection.update(selector, token, WriteConcern.Acknowledged, upsert = true)
  }

  def removeToken(tokenId: String): Future[WriteResult] = {
    val selector = BSONDocument("tokenId" -> tokenId)
    tokensCollection.remove(selector, WriteConcern.Acknowledged)
  }
}
