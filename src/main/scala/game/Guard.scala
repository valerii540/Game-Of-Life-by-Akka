package game

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import game.cellADT._
import ui.ConsoleWriter

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Random, Try}

object Guard {
  type Board = IndexedSeq[IndexedSeq[ActorRef[CellEvent]]]

  def apply(rows: Int, columns: Int): Behavior[Unit] =
    Behaviors.setup { implicit ctx =>
      implicit val ec: ExecutionContext = ctx.executionContext
      implicit val scheduler: Scheduler = ctx.system.scheduler

      val board  = spawnCells(rows, columns)
      val actors = board.flatten

      var epoch: Long = 0
      ctx.system.scheduler.scheduleAtFixedRate(2.seconds, 200.millis) { () =>
        epoch += 1
        val f =
          ConsoleWriter.write(board, epoch).flatMap { _ =>
            Future
              .traverse(actors)(a => a.ask(NextState)(2.seconds, scheduler).map(a -> _))
              .flatMap { as =>
                Future.traverse(as) { case (actor, nextState) =>
                  actor.ask(r => MutateTo(r, nextState))(2.seconds, scheduler)
                }
              }
          }

        Await.result(f, 5.seconds)
      }

      Behaviors.empty
    }

  private def spawnCells(rows: Int, columns: Int)(implicit ctx: ActorContext[_]): Board = {
    def deadOrAlive() = if (Random.nextInt(100) < 20) Alive else Dead

    val board = IndexedSeq.tabulate(rows, columns)((r, c) => ctx.spawn(Cell(deadOrAlive()), s"cell-$r-$c"))

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

    board
  }
}
