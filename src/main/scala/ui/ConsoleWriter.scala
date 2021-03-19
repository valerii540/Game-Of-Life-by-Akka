package ui

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import game.Guard.Board
import game.cellADT._

import scala.Console._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ConsoleWriter {
  private val clearScreen = "\u001b[2J"
  private val aliveSign   = s"$RESET$GREEN_B   $RESET"
  private val deadSign    = s"$RESET$BLACK_B   $RESET"

  private implicit val timeout: Timeout = 2.seconds

  def write(board: Board, epoch: Long)(implicit scheduler: Scheduler, ec: ExecutionContext): Future[Unit] = {
    println(clearScreen)
    println(s"EPOCH: $epoch")
    Future
      .traverse(board) { row =>
        Future
          .sequence(row.map(a => a.ask(ShowState)))
          .map { states =>
            val formattedRow = states.map {
              case Alive => aliveSign
              case Dead  => deadSign
            }
            println(formattedRow.mkString)
          }
      }.map(_ => ())
  }
}
