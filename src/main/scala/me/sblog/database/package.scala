package me.sblog

import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

package object database {
  def withDb(dbConnection: MongoConnection, dbName: String)
            (onSuccess: DefaultDB => Unit)(onFailure: Throwable => Unit): Unit = {
    dbConnection.database(dbName).onComplete {
      case Success(db) =>
        onSuccess(db)
      case Failure(e) =>
        onFailure(e)
    }
  }
}
