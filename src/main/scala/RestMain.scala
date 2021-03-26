import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{AttributeKeys, HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import game.Game
import game.Response.asJson
import kamon.Kamon

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object RestMain {
  def main(args: Array[String]): Unit = {
//    Kamon.init()

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "universe")
    implicit val ex: ExecutionContext         = system.executionContext
    implicit val timeout: Timeout             = 3.seconds
    implicit val scheduler: Scheduler         = system.scheduler

//    scheduler.scheduleAtFixedRate(5.seconds, 1.second) { () => Await.result(system.ask(Mutate), 4.seconds) }

    val game = new Game(args(0).toInt, args(1).toInt)

    val sink = Sink.foreach[Message](v => println(s"Client message: $v"))
    val source =
      Source
        .tick(5.seconds, 1.second, ())
        .map(_ => TextMessage.Strict(asJson(game.mutate())))

    val requestHandler: HttpRequest => HttpResponse = {
      case req @ HttpRequest(GET, Uri.Path("/board"), _, _, _) =>
        req.attribute(AttributeKeys.webSocketUpgrade) match {
          case Some(upgrade) => upgrade.handleMessagesWithSinkSource(sink, source)
          case None          => HttpResponse(400, entity = "Not a valid websocket request!")
        }
      case r: HttpRequest =>
        r.discardEntityBytes()
        HttpResponse(400, entity = "Unknown resource!")
    }

    val binding = Await.result(Http().newServerAt("localhost", 8080).bindSync(requestHandler), 5.seconds)

    sys.addShutdownHook {
      binding
        .unbind()
        .onComplete(_ => Kamon.stop().onComplete(_ => system.terminate()))
    }
  }
}

object Kek extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration.DurationInt
  import scala.concurrent.{Await, Future}

  val f =
    for {
      r <- Future(1)
      _ = Future { Thread.sleep(3000); throw new Exception("!!!") }
//        .recover(_ => println("At this moment he fucked up"))
    } yield r

  Await.result(f, 1.second)

  Thread.sleep(7000)

}
