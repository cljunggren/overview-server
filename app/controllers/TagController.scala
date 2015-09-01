package controllers

import com.opencsv.CSVWriter
import play.api.libs.concurrent.Execution.Implicits._
import java.io.StringWriter
import scala.concurrent.Future

import controllers.auth.AuthorizedAction
import controllers.auth.Authorities.userOwningDocumentSet
import controllers.backend.{SelectionBackend,TagBackend}
import controllers.forms.TagForm
import com.overviewdocs.models.Tag

trait TagController extends Controller {
  protected val tagBackend: TagBackend
  private val selectionIdKey = "selectionId"

  def create(documentSetId: Long) = AuthorizedAction(userOwningDocumentSet(documentSetId)).async { implicit request =>
    TagForm.forCreate.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest),
      attributes => {
        tagBackend.create(documentSetId, attributes)
          .map(tag => Created(views.json.Tag.create(tag)))
      }
    )
  }

  def indexJson(documentSetId: Long) = AuthorizedAction.inTransaction(userOwningDocumentSet(documentSetId)).async { implicit request =>
    for {
      tagsWithCounts <- tagBackend.indexWithCounts(documentSetId)
    } yield Ok(views.json.Tag.index.withCounts(tagsWithCounts))
      .withHeaders(CACHE_CONTROL -> "max-age=0")
  }

  def indexCsv(documentSetId: Long) = AuthorizedAction.inTransaction(userOwningDocumentSet(documentSetId)).async { implicit request =>
    for {
      tagsWithCounts <- tagBackend.indexWithCounts(documentSetId)
    } yield {
      val stringWriter = new StringWriter()
      val csvWriter = new CSVWriter(stringWriter)
      csvWriter.writeNext(Array("id", "name", "count", "color"))
      tagsWithCounts.foreach({ case (tag, count) =>
        csvWriter.writeNext(Array(tag.id.toString, tag.name, count.toString, tag.color))
      })
      csvWriter.close()
      Ok(stringWriter.toString())
        .as("text/csv")
        .withHeaders(
          CACHE_CONTROL -> "max-age=0",
          CONTENT_DISPOSITION -> "attachment; filename=overview-tags.csv"
        )
    }
  }

  def destroy(documentSetId: Long, tagId: Long) = AuthorizedAction(userOwningDocumentSet(documentSetId)).async { implicit request =>
    tagBackend.destroy(documentSetId, tagId)
      .map(_ => NoContent)
  }

  def update(documentSetId: Long, tagId: Long) = AuthorizedAction(userOwningDocumentSet(documentSetId)).async { implicit request =>
    TagForm.forUpdate.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest),
      attributes => {
        tagBackend.update(documentSetId, tagId, attributes).map(_ match {
          case None => NotFound
          case Some(tag) => Ok(views.json.Tag.update(tag))
        })
      }
    )
  }
}

object TagController extends TagController {
  override protected val tagBackend = TagBackend
}
