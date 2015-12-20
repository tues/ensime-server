package org.ensime.core

import akka.actor._
import akka.event.LoggingReceive.withLabel
import org.ensime.api._
import org.ensime.core.javac._
import org.ensime.indexer.{ EnsimeVFS, SearchService }
import org.ensime.util.ReportHandler
import org.ensime.util.FileUtils

class JavaAnalyzer(
    broadcaster: ActorRef,
    search: SearchService,
    implicit val config: EnsimeConfig,
    implicit val vfs: EnsimeVFS
) extends Actor with Stash with ActorLogging {

  protected var javaCompiler: JavaCompiler = _

  import FileUtils._

  override def preStart(): Unit = {
    javaCompiler = new JavaCompiler(
      config,
      new ReportHandler {
        override def messageUser(str: String): Unit = {
          broadcaster ! SendBackgroundMessageEvent(str, 101)
        }
        override def clearAllJavaNotes(): Unit = {
          broadcaster ! ClearAllJavaNotesEvent
        }
        override def reportJavaNotes(notes: List[Note]): Unit = {
          broadcaster ! NewJavaNotesEvent(isFull = false, notes)
        }
      },
      search,
      vfs
    )

    // JavaAnalyzer is always 'ready', but legacy clients expect to see
    // AnalyzerReady
    broadcaster ! Broadcaster.Persist(AnalyzerReadyEvent)
  }

  override def postStop(): Unit = {
    // no way to stop the java compiler
  }

  // TODO: create a sealed family of requests / responses just for Java usage
  override def receive = {
    case TypecheckFileReq(sfi) =>
      javaCompiler.askTypecheckFiles(sfi :: Nil)
      sender() ! VoidResponse

    case TypecheckFilesReq(files) =>
      javaCompiler.askTypecheckFiles(files.map(toSourceFileInfo))
      sender() ! VoidResponse

    case CompletionsReq(file, point, maxResults, caseSens, _) =>
      sender() ! javaCompiler.askCompletionsAtPoint(file, point, maxResults, caseSens)

    case DocUriAtPointReq(file, range) =>
      sender() ! javaCompiler.askDocSignatureAtPoint(file, range.from)

    case TypeAtPointReq(file, range) =>
      sender() ! javaCompiler.askTypeAtPoint(file, range.from)

    case SymbolDesignationsReq(f, start, end, tpes) =>
      // NOT IMPLEMENTED YET
      sender ! SymbolDesignations(f.file, Nil)

    case SymbolAtPointReq(file, point) =>
      sender() ! javaCompiler.askSymbolAtPoint(file, point)

  }

}
object JavaAnalyzer {
  def apply(
    broadcaster: ActorRef,
    search: SearchService
  )(
    implicit
    config: EnsimeConfig,
    vfs: EnsimeVFS
  ) = Props(new JavaAnalyzer(broadcaster, search, config, vfs))
}