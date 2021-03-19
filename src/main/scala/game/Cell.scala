package game
import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.Cell.Neighbors

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object cellADT {
  sealed trait CellEvent

  case class NextState(replyTo: ActorRef[State])             extends CellEvent
  case class MutateTo(replyTo: ActorRef[Done], state: State) extends CellEvent

  /** Service messages */
  case class SetNeighbors(neighbors: Neighbors)  extends CellEvent
  case class ShowState(replyTo: ActorRef[State]) extends CellEvent

  /** Cell state */
  sealed trait State
  case object Alive extends State
  case object Dead  extends State
}

object Cell {
  import Behaviors._
  import cellADT._

  private case object Revive extends CellEvent
  private case object Die    extends CellEvent

  type Neighbors = Set[ActorRef[CellEvent]]

  def apply(initialState: State): Behavior[CellEvent] =
    initialState match {
      case Alive => alive(Set.empty)
      case Dead  => dead(Set.empty)
    }

  private def alive(neighbors: Neighbors): Behavior[CellEvent] = logMessages {
    receiveMessage {
      case Die                => dead(neighbors)
      case Revive             => same
      case NextState(replyTo) => prepare(replyTo, neighbors, Alive)
      case ShowState(replyTo) => replyTo ! Alive; same
      case SetNeighbors(n)    => alive(n)
      case unknown            => fuckedUp("alive", unknown)
    }
  }

  private def dead(neighbors: Neighbors): Behavior[CellEvent] = logMessages {
    receiveMessage {
      case Revive             => alive(neighbors)
      case Die                => same
      case NextState(replyTo) => prepare(replyTo, neighbors, Dead)
      case ShowState(replyTo) => replyTo ! Dead; same
      case SetNeighbors(n)    => dead(n)
      case unknown            => fuckedUp("dead", unknown)
    }
  }

  private def mutation(neighbors: Neighbors, state: State): Behavior[CellEvent] = logMessages {
    receiveMessage {
      case ShowState(replyTo)       => replyTo ! state; same
      case MutateTo(replyTo, Alive) => replyTo ! Done; alive(neighbors)
      case MutateTo(replyTo, Dead)  => replyTo ! Done; dead(neighbors)
      case unknown                  => fuckedUp("mutation", unknown)
    }
  }

  private def prepare(replyTo: ActorRef[State], neighbors: Neighbors, state: State): Behavior[CellEvent] = logMessages {
    setup { ctx =>
      implicit val ec: ExecutionContext = ctx.executionContext

      Future
        .sequence(neighbors.toSeq.map(n => n.ask(ShowState)(2.seconds, ctx.system.scheduler)))
        .map(_.count(_ == Alive))
        .onComplete {
          case Success(n) if n <= 1 || n >= 4 => replyTo ! Dead
          case Success(n) if n == 3           => replyTo ! Alive
          case Success(_)                     => replyTo ! state
          case Failure(exception)             => throw exception
        }

      mutation(neighbors, state)
    }
  }

  private def fuckedUp(from: String, message: Any): Behavior[CellEvent] = setup { ctx =>
    ctx.log.error("[{}] at this moment {} fucked up: {}", from, ctx.self.path.name, message)
    Behaviors.stopped
  }
}
