package kk

import scala.concurrent.duration._
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor


class LoggingActor extends PersistentActor with ActorLogging {
  context.setReceiveTimeout(1 millisecond)

  val maxBufferLen = 10
  var buffer: List[Message] = List.empty

  def flush() = if (buffer.length > 0) {
    log.info(buffer.toString)
    buffer = List.empty
  }

  def processMessage(m: Message) = {
    if (buffer.length >= maxBufferLen) {
      flush
    }
    buffer :+= m
  }

  override def persistenceId: String = "Logger-" + self.path.name

  override def receiveRecover: Receive = {
    case m: Message => buffer :+= m
  }

  override def receiveCommand: Receive = {
    case m: Message => {
      processMessage(m)
      sender ! true
    }
    case ReceiveTimeout => flush
  }
}

object LoggingActor {
  val shardName = "logger"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case m @ Message(id) => (id.toString, m)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case Message(id) => id.toString
  }
}
