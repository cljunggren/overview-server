package com.overviewdocs.clone

import scala.concurrent.Future

import com.overviewdocs.database.HasDatabase
import com.overviewdocs.searchindex.{ElasticSearchIndexClient,IndexClient}
import com.overviewdocs.models.Document
import com.overviewdocs.models.tables.Documents

trait Indexer {
  protected val indexClient: IndexClient
  def indexDocuments(documentSetId: Long): Future[Unit]
}

object Indexer extends Indexer with HasDatabase {
  import database.api._
  import database.executionContext

  override protected val indexClient = ElasticSearchIndexClient.singleton

  private val NDocumentsPerBatch = 30 // ~1MB/document max

  def indexDocuments(documentSetId: Long): Future[Unit] = {
    for {
      _ <- indexClient.addDocumentSet(documentSetId)
      _ <- indexEachDocument(documentSetId)
    } yield ()
  }

  private def indexRemainingBatches(idsIt: Iterator[Seq[Long]]): Future[Unit] = {
    if (idsIt.hasNext) {
      val ids: Seq[Long] = idsIt.next
      val step: Future[Unit] = for {
        documents <- database.seq(Documents.filter(_.id inSet ids))
        _ <- indexClient.addDocuments(documents)
      }  yield ()
      step.flatMap(_ => indexRemainingBatches(idsIt))
    } else {
      Future.successful(())
    }
  }

  private def indexEachDocument(documentSetId: Long): Future[Unit] = {
    database.seq(Documents.filter(_.documentSetId === documentSetId).map(_.id)).flatMap { allIds =>
      val idsIt = allIds.grouped(NDocumentsPerBatch)
      indexRemainingBatches(idsIt)
    }
  }
}
