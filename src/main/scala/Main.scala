import akka.actor.typed.ActorSystem
import game.Guard

object Main {

  def main(args: Array[String]): Unit =
    ActorSystem(Guard(args(0).toInt, args(1).toInt), "universe")

}
