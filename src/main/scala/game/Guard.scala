package game

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import game.cellADT._
import monitoring.KamonReporter

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

object Guard {
  type MatrixOfActors = IndexedSeq[IndexedSeq[ActorRef[CellEvent]]]

  sealed trait GuardEvent
  case class ShowBoard(replyTo: ActorRef[Response]) extends GuardEvent

  private var epoch = 1L

  def apply(rows: Int, columns: Int): Behavior[GuardEvent] = Behaviors.setup { implicit ctx =>
    implicit val ec: ExecutionContext = ctx.executionContext
    implicit val scheduler: Scheduler = ctx.system.scheduler

    val matrix = spawnCells(rows, columns)
    val actors = matrix.flatten

    ctx.system.scheduler
      .scheduleAtFixedRate(5.seconds, 1000.millis)(() => Await.result(mutateAllAsync(actors), 5.seconds))

    Behaviors.receiveMessage { case ShowBoard(replyTo) =>
      Future
        .traverse(matrix)(row => Future.sequence(row.map(a => a.ask(ShowState)(2.seconds, ctx.system.scheduler))))
        .onComplete {
          case Success(matrixOfStates) => replyTo ! Response(epoch, matrixOfStates)
          case Failure(exception)      => throw exception
        }

      Behaviors.same
    }
  }

  private def spawnCells(rows: Int, columns: Int)(implicit ctx: ActorContext[_]): MatrixOfActors = {
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

  private def mutateAllAsync(actors: Seq[ActorRef[CellEvent]])(implicit
      ex: ExecutionContext,
      scheduler: Scheduler
  ): Future[Unit] = {
    val timer = KamonReporter.mutationLatencyTimer.start()
    Future
      .traverse(actors)(a => a.ask(NextState)(2.seconds, scheduler).map(a -> _))
      .flatMap { as =>
        Future.traverse(as) { case (actor, nextState) =>
          actor.ask(r => MutateTo(r, nextState))(2.seconds, scheduler)
        }
      }.map { _ =>
        epoch += 1
        timer.stop()
      }
  }
}
