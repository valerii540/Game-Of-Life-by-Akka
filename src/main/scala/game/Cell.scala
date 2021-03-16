package game
import akka.actor.typed.{ActorRef, Behavior}

object cellADT {
  sealed trait CellEvent

  /** Game rules */
  case object Revive extends CellEvent
  case object Die    extends CellEvent

  /** Service messages */
  case object ShowUp                             extends CellEvent
  case class ShowState(replyTo: ActorRef[State]) extends CellEvent

  /** Cell state */
  sealed trait State
  case object Alive extends State
  case object Dead  extends State
}

object Cell {
  import akka.actor.typed.scaladsl.Behaviors._
  import cellADT._

  def apply(initialState: State): Behavior[CellEvent] =
    initialState match {
      case Alive => alive
      case Dead  => dead
    }

  private def alive: Behavior[CellEvent] = receiveMessage {
    case Die                => dead
    case Revive             => same
    case ShowState(replyTo) =>
      replyTo ! Alive
      same
    case ShowUp             => showUp(Alive)
    case _                  => unhandled
  }

  private def dead: Behavior[CellEvent] = receiveMessage {
    case Revive             => alive
    case Die                => same
    case ShowState(replyTo) =>
      replyTo ! Dead
      same
    case ShowUp             => showUp(Dead)
    case _                  => unhandled
  }

  private def showUp(state: State): Behavior[CellEvent] = setup { ctx =>
    ctx.log.info(s"I am ${ctx.self.path.name} and I am $state")
    same
  }
}
