package ui

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.util.Timeout
import game.Guard.Board
import game.cellADT._

import scala.Console._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

final class ConsoleWriter(board: Board, system: ActorSystem[_]) {
  import ConsoleWriter._

  private implicit val timeout: Timeout     = 2.seconds
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val ec: ExecutionContext = system.executionContext

  def write(): Unit = {
    println(clearScreen)
    board.foreach { row =>
      val states       = Await.result(Future.sequence(row.map(a => a.ask(ShowState))), 5.seconds)
      val formattedRow = states.map {
        case Alive => aliveSign
        case Dead  => deadSign
      }
      println(formattedRow.mkString)
    }
  }
}

object ConsoleWriter {
  private val clearScreen = "\u001b[2J"
  private val aliveSign   = s"$RESET$GREEN_B   $RESET"
  private val deadSign    = s"$RESET$BLACK_B   $RESET"
}
