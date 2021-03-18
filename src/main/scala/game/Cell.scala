package game
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.Cell.Neighbors

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object cellADT {
  sealed trait CellEvent

  case object Mutate extends CellEvent

  /** Service messages */
  case class SetNeighbors(neighbors: Neighbors)  extends CellEvent
  case class ShowState(replyTo: ActorRef[State]) extends CellEvent
  case object ShowUp                             extends CellEvent

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

  def apply(initialState: State, topic: ActorRef[Command[CellEvent]]): Behavior[CellEvent] = Behaviors.setup { ctx =>
    topic ! Topic.Subscribe(ctx.self)

    initialState match {
      case Alive => alive(Set.empty)
      case Dead  => dead(Set.empty)
    }
  }

  private def alive(neighbors: Neighbors): Behavior[CellEvent] = logMessages {
    receiveMessage {
      case Die                => dead(neighbors)
      case Revive             => same
      case Mutate             => mutate(Alive, neighbors)
      case ShowState(replyTo) =>
        replyTo ! Alive
        same
      case SetNeighbors(n)    => alive(n)
      case _                  => unhandled
    }
  }

  private def dead(neighbors: Neighbors): Behavior[CellEvent] = logMessages {
    receiveMessage {
      case Revive             => alive(neighbors)
      case Die                => same
      case Mutate             => mutate(Dead, neighbors)
      case ShowState(replyTo) =>
        replyTo ! Dead
        same
      case SetNeighbors(n)    => dead(n)
      case _                  => unhandled
    }
  }

  private def mutate(state: State, neighbors: Neighbors): Behavior[CellEvent] = logMessages {
    setup { ctx =>
      implicit val ec: ExecutionContext = ctx.executionContext

      Future
        .sequence(neighbors.toSeq.map(n => n.ask(ShowState)(2.seconds, ctx.system.scheduler)))
        .map(n => state -> n.filterNot(_ == Dead).size)
        .onComplete {
          case Success(Alive -> n) if n <= 1 || n >= 4 => ctx.self ! Die
          case Success(Dead -> n) if n == 3            => ctx.self ! Revive
          case Success(_)                              => ()
          case Failure(exception)                      => throw exception
        }
      same
    }
  }
}
