package views.json.helper

import models.PersistentTagInfo
import models.core.{ Document, DocumentIdList }
import play.api.libs.json.{ JsValue, Writes }
import play.api.libs.json.Json.toJson

object ModelJsonConverters {

  implicit object JsonDocumentIdList extends Writes[DocumentIdList] {
    override def writes(documentIdList: DocumentIdList): JsValue = {
      toJson(Map(
        "docids" -> toJson(documentIdList.firstIds),
        "n" -> toJson(documentIdList.totalCount)))
    }
  }

  implicit object JsonPersistentTagInfo extends Writes[PersistentTagInfo] {
    override def writes(tag: PersistentTagInfo): JsValue = {
      val tagValues = Map(
        "id" -> toJson(tag.id),
        "name" -> toJson(tag.name),
        "doclist" -> toJson(tag.documentIds))

      val tagColorValue = maybeMap("color", tag.color, '#' + _) 

      toJson(tagValues ++ tagColorValue)
    }
  }

  implicit object JsonDocument extends Writes[Document] {
    override def writes(document: Document): JsValue = {
      val documentValues = Map(
        "id" -> toJson(document.id),
        "description" -> toJson(document.description),
        "tagids" -> toJson(document.tags),
        "documentcloud_id" -> toJson(document.documentCloudId),
        "nodeids" -> toJson(document.nodes)) 
        
      val titleValue = maybeMap("title", document.title)
      
      toJson(documentValues ++ titleValue)
    }
  }
  
  private def maybeMap(key: String, maybeValue: Option[String], toValue: String => String = identity): Map[String, JsValue] =
    maybeValue.map(v => Map(key -> toJson(toValue(v)))).getOrElse(Map.empty)
    

}
