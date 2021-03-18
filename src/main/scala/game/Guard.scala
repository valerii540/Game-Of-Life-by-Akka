package game

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.cellADT._
import ui.ConsoleWriter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Random, Try}

object Guard {
  type Board = IndexedSeq[IndexedSeq[ActorRef[CellEvent]]]

  def apply(rows: Int, columns: Int): Behavior[Unit] =
    Behaviors.setup { ctx =>
      implicit val ec: ExecutionContext = ctx.executionContext

      val topic = ctx.spawn(Topic[CellEvent]("topic"), "master-of-puppets")

      val board = IndexedSeq.tabulate(rows, columns)((r, c) => ctx.spawn(Cell(deadOrAlive, topic), s"cell-$r-$c"))

      setNeighbors(board)

      val consoleUI = new ConsoleWriter(board, ctx.system)

      ctx.system.scheduler.scheduleAtFixedRate(2.seconds, 300.millis) { () =>
        consoleUI.write()
        topic ! Topic.Publish(Mutate)
      }

      Behaviors.empty
    }

  private def setNeighbors(board: Board): Unit =
    for {
      row       <- board.indices
      col       <- board(row).indices
      neighbours =
        Set(
          Try(board(row - 1)(col + 1)).toOption, // TR
          Try(board(row - 1)(col - 1)).toOption, // TL
          Try(board(row)(col - 1)).toOption,     // L
          Try(board(row)(col + 1)).toOption,     // R
          Try(board(row + 1)(col + 1)).toOption, // BR
          Try(board(row + 1)(col - 1)).toOption, // BL
          Try(board(row - 1)(col)).toOption,     // B
          Try(board(row + 1)(col)).toOption      // T
        ).flatten
    } board(row)(col) ! SetNeighbors(neighbours)

  private def deadOrAlive: State = if (Random.nextInt(100) < 10) Alive else Dead
}
