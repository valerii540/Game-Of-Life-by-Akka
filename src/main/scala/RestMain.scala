import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, onComplete, path}
import akka.util.Timeout
import game.Guard
import game.Guard.{GuardEvent, ShowBoard}
import game.Response.asJson
import kamon.Kamon

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object RestMain {
  def main(args: Array[String]): Unit = {
    Kamon.init()

    implicit val system: ActorSystem[GuardEvent] = ActorSystem(Guard(args(0).toInt, args(1).toInt), "universe")
    implicit val aksTimeout: Timeout             = 5.seconds
    implicit val ex: ExecutionContext            = system.executionContext
    implicit val scheduler: Scheduler            = system.scheduler

    val route = path("board") {
      onComplete(system.ask(ShowBoard)) {
        case Success(response)  =>
          complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, asJson(response))))
        case Failure(exception) =>
          throw exception
      }
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => Kamon.stop().onComplete(_ => system.terminate()))
    }
  }
}
