package me.lsbengine.api.admin

import me.lsbengine.database.DatabaseAccessor
import me.lsbengine.database.model.{MongoCollections, Image}
import me.lsbengine.database.model.MongoFormats.imageFormat
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.{FileInputStream, FileOutputStream, File}

class ImagesAccessor(database: DefaultDB)
  extends DatabaseAccessor[Image](database, MongoCollections.imagesCollectionName) {

  def getImage(name: String): Future[Option[Image]] = {
    super.getItem(BSONDocument("name" -> name))
  }

  def saveImage(image: Image, file: File): Future[UpdateWriteResult] = {
    val dest = new File(image.path)
    copyFile(file, dest)
    file.delete()
    super.upsertItem(BSONDocument("name" -> image.name), image)
  }

  def getImages: Future[List[Image]] = {
    super.getItems(BSONDocument())
  }

  def deleteImage(name: String): Future[UpdateWriteResult] = {
    val selector = BSONDocument("name" -> name)
    getCollection.findAndRemove(selector).flatMap {
      res =>
        res.result[Image] match {
          case Some(image) =>
            val ok = new File(image.path).delete()
            Future(UpdateWriteResult(ok = ok, 0, 0, Seq(), Seq(), None, None, None))
          case None =>
            Future(UpdateWriteResult(ok = false, 0, 0, Seq(), Seq(), None, None, None))
        }
    }
  }

  private def copyFile(source: File, dest: File) = {
      val is = new FileInputStream(source);
      val os = new FileOutputStream(dest);
      val bs = Stream.continually(is.read).takeWhile(_ != -1)

      bs.foreach(os.write(_))

      is.close();
      os.close();
  }
}
