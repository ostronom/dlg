package kk

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor._
import akka.util.Timeout
import akka.cluster.sharding.ClusterSharding
import cats.data.Xor
import akka.pattern.ask
import java.nio.file.{Files, Paths}


case class ReadRequest(path: String)


class ReaderActor extends Actor {
  var clusterReady = false
  val timeout = 20 seconds
  implicit val akkaTimeout = Timeout(timeout)

  def parse(path: String) = {
    val contents = new String(Files.readAllBytes(Paths.get(path)))
    val logging = ClusterSharding(context.system).shardRegion(LoggingActor.shardName)
    Message.parseList(new String(contents)) match {
      case Xor.Left(err) => println(err)
      case Xor.Right(msgs) => msgs.foreach { msg =>
        if (clusterReady) {
          logging ! msg
        } else {
          Await.result(logging ? msg , timeout)
          clusterReady = true
        }
      }
    }
  }

  def receive = {
    case ReadRequest(path) => parse(path)
  }
}
