import akka.actor.typed.ActorSystem
import game.Guard

object Main extends App {
  ActorSystem(Guard(), "universe")
}
