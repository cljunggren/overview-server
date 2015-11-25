package controllers.backend

import scala.concurrent.Future

import com.overviewdocs.models.{CsvImport,CsvImportJob,DocumentSet,DocumentSetCreationJob,DocumentSetCreationJobImportJob,DocumentSetCreationJobState,DocumentSetCreationJobType,DocumentSetUser,FileGroup,FileGroupImportJob,ImportJob}
import com.overviewdocs.models.tables.{CsvImports,DocumentSetCreationJobs,DocumentSetUsers,DocumentSets,DocumentSetsImpl,FileGroups,FileGroupsImpl}

trait ImportJobBackend extends Backend {
  /** All ImportJobs for the user. */
  def indexByUser(userEmail: String): Future[Seq[ImportJob]]

  /** All ImportJobs. */
  def indexWithDocumentSetsAndOwners: Future[Seq[(ImportJob,DocumentSet,Option[String])]]

  /** All ImportJobs for the given DocumentSet. */
  def indexByDocumentSet(documentSetId: Long): Future[Seq[ImportJob]]
}

trait DbImportJobBackend extends ImportJobBackend with DbBackend {
  import database.api._
  import database.executionContext

  private lazy val csvImportsByUserEmail = Compiled { userEmail: Rep[String] =>
    val documentSetIds = DocumentSetUsers.filter(_.userEmail === userEmail).map(_.documentSetId)

    CsvImports
      .filter(_.documentSetId in documentSetIds)
      .filter(ci => ci.nBytesProcessed < ci.nBytes)
  }

  private lazy val fileGroupsByUserEmail = Compiled { userEmail: Rep[String] =>
    val documentSetIds = DocumentSetUsers.filter(_.userEmail === userEmail).map(_.documentSetId)

    FileGroups
      .filter(_.addToDocumentSetId in documentSetIds)
  }

  private lazy val dscjsByUserEmail = Compiled { userEmail: Rep[String] =>
    val documentSetIds = DocumentSetUsers.filter(_.userEmail === userEmail).map(_.documentSetId)

    DocumentSetCreationJobs
      .filter(_.state =!= DocumentSetCreationJobState.Cancelled)
      .filter(_.documentSetId in documentSetIds)
  }

  private lazy val csvImportsByDocumentSetId = Compiled { documentSetId: Rep[Long] =>
    CsvImports
      .filter(_.documentSetId === documentSetId)
      .filter(ci => ci.nBytesProcessed < ci.nBytes)
  }

  private lazy val fileGroupsByDocumentSetId = Compiled { documentSetId: Rep[Long] =>
    FileGroups
      .filter(_.addToDocumentSetId === documentSetId)
  }

  private lazy val dscjsByDocumentSetId = Compiled { documentSetId: Rep[Long] =>
    DocumentSetCreationJobs
      .filter(_.documentSetId === documentSetId)
      .filter(_.state =!= DocumentSetCreationJobState.Cancelled)
  }

  private lazy val csvImportsWithDocumentSetsAndOwners = {
    CsvImports
      .filter(ci => ci.nBytesProcessed < ci.nBytes)
      .join(DocumentSets)
        .on(_.documentSetId === _.id)
      .joinLeft(DocumentSetUsers.filter(_.role === DocumentSetUser.Role(true)))
        .on(_._1.documentSetId === _.documentSetId)
      .map(t => (t._1._1, t._1._2, t._2.map(_.userEmail)))
  }

  private lazy val fileGroupsWithDocumentSetsAndOwners = {
    FileGroups
      .filter(_.addToDocumentSetId.nonEmpty)
      .join(DocumentSets)
        .on(_.addToDocumentSetId === _.id)
      .joinLeft(DocumentSetUsers.filter(_.role === DocumentSetUser.Role(true)))
        .on(_._1.addToDocumentSetId === _.documentSetId)
      .map(t => (t._1._1, t._1._2, t._2.map(_.userEmail)))
  }

  private lazy val dscjsWithDocumentSetsAndOwners = {
    DocumentSetCreationJobs
      .filter(_.state =!= DocumentSetCreationJobState.Cancelled)
      .join(DocumentSets)
        .on(_.documentSetId === _.id)
      .joinLeft(DocumentSetUsers.filter(_.role === DocumentSetUser.Role(true)))
        .on(_._1.documentSetId === _.documentSetId)
      .map(t => (t._1._1, t._1._2, t._2.map(_.userEmail)))
  }

  override def indexByUser(userEmail: String) = {
    for {
      jobs1 <- database.seq(csvImportsByUserEmail(userEmail))
      jobs2 <- database.seq(fileGroupsByUserEmail(userEmail))
      jobs3 <- database.seq(dscjsByUserEmail(userEmail))
    } yield {
      jobs1.map(CsvImportJob.apply _)
        .++(jobs2.map(FileGroupImportJob.apply _))
        .++(jobs3.map(DocumentSetCreationJobImportJob.apply _))
    }
  }

  override def indexByDocumentSet(documentSetId: Long) = {
    for {
      jobs1 <- database.seq(csvImportsByDocumentSetId(documentSetId))
      jobs2 <- database.seq(fileGroupsByDocumentSetId(documentSetId))
      jobs3 <- database.seq(dscjsByDocumentSetId(documentSetId))
    } yield {
      jobs1.map(CsvImportJob.apply _)
        .++(jobs2.map(FileGroupImportJob.apply _))
        .++(jobs3.map(DocumentSetCreationJobImportJob.apply _))
    }
  }

  override def indexWithDocumentSetsAndOwners = {
    for {
      jobs1 <- database.seq(csvImportsWithDocumentSetsAndOwners)
      jobs2 <- database.seq(fileGroupsWithDocumentSetsAndOwners)
      jobs3 <- database.seq(dscjsWithDocumentSetsAndOwners)
    } yield {
      jobs1.map(t => (CsvImportJob(t._1), t._2, t._3))
        .++(jobs2.map(t => (FileGroupImportJob(t._1), t._2, t._3)))
        .++(jobs3.map(t => (DocumentSetCreationJobImportJob(t._1), t._2, t._3)))
    }
  }
}

object ImportJobBackend extends DbImportJobBackend
