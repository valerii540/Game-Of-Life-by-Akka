package ui

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import game.cellADT._
import ui.ConsoleUI.Board

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

final class ConsoleUI(board: Board, system: ActorSystem[Nothing]) {
  private[this] implicit val timeout: Timeout     = 2.seconds
  private[this] implicit val scheduler: Scheduler = system.scheduler
  private[this] implicit val ec: ExecutionContext = system.executionContext

  def write(): Unit = {
    val border = "=" * (board.head.size * 2 + 1)
    println(border)
    board.foreach { row =>
      val states    = Await.result(Future.sequence(row.map(a => a.ask(ShowState))), 5.seconds)
      val formatted = states.map {
        case Alive => 'O'
        case Dead  => 'X'
      }
      println("|" + formatted.mkString("|") + "|")
    }
    println(border)
    println()
  }
}

object ConsoleUI {
  type Board = IndexedSeq[IndexedSeq[ActorRef[CellEvent]]]
}
