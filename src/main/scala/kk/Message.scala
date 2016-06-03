package kk

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import cats.data.Xor


case class Message(id: Int)

object Message {
  def parseList(msg: String) = decode[List[Message]](msg)
}
