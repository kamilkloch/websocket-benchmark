import cats.effect.*
import cats.effect.std.Supervisor
import config.WebServerConfig
import config.WebServerConfig.{responseStreamFromTopic, service, tsService}
import fs2.concurrent.Topic
import org.http4s.websocket.WebSocketFrame

object Http4sBlaze extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = {
    Supervisor[IO].use { sup =>
      for {
        tsTopic <- Topic[IO, WebSocketFrame]
        _ <- tsService(tsTopic).supervise(sup)
//        _ <- tsTopic.subscribers.foreach(x => IO.println(s"Subscriber count: $x")).compile.drain.supervise(sup)
        _ <- WebServerConfig.blaze.serverResource(service(responseStreamFromTopic(tsTopic))).useForever
      } yield ()
    }
  }
}
