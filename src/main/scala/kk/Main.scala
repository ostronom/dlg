package kk

import scala.concurrent.duration._
import scala.concurrent.Future
import java.io.File
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.pattern.ask
import akka.routing.SmallestMailboxPool
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout


object Main extends App {
  val parFactor = 8
  val storePath = "akka.tcp://kk-system@127.0.0.1:2551/user/store"
  val config = ConfigFactory.load()
  val system = ActorSystem("kk-system", config)
  import system.dispatcher
  implicit val timeout = Timeout(15 seconds)

  def registerJournal(): Future[Any] = {
    val f = (system.actorSelection(storePath) ? Identify(None))
    f onSuccess {
      case ActorIdentity(_, Some(ref)) =>
        system.log.info("Shared journal started")
        SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started")
        system.terminate()
    }
    f onFailure {
      case _ =>
        system.log.error("Lookup of shared journal timed out")
        system.terminate()
    }
    f
  }

  def startParsing() = {
    val dir = new File("json").listFiles
    val reader = system.actorOf(SmallestMailboxPool(parFactor).props(Props[ReaderActor]))
    dir.view
      .filter(!_.isDirectory)
      .map(_.toString)
      .filter(_.endsWith(".json"))
      .map(ReadRequest(_))
      .foreach { d => reader ! d }
  }

  def startCluster(): Future[Any] = {
    val cluster = ClusterSharding(system).start(
      typeName = LoggingActor.shardName,
      entityProps = Props[LoggingActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = LoggingActor.extractEntityId,
      extractShardId = LoggingActor.extractShardId
    )
    (system.actorSelection(cluster.path) ? Identify(None))
  }

  system.actorOf(Props[SharedLeveldbStore], "store")
  for {
    _ <- registerJournal
    _ <- startCluster
  } {
    startParsing
  }
}
