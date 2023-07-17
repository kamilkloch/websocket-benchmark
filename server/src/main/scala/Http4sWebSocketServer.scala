import cats.effect.*
import com.comcast.ip4s.IpLiteralSyntax
import fs2.*
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.duration.*

object Http4sWebSocketServer extends IOApp.Simple {

  private val connectorPoolSize = Math.max(2, Runtime.getRuntime.availableProcessors() / 4)
  private val responseStream = Stream.repeatEval(IO.realTime.map(ts => WebSocketFrame.Text(s"${ts.toMillis}")).delayBy(500.millis))

  override protected def computeWorkerThreadCount: Int =
    Math.max(2, super.computeWorkerThreadCount / 2)

  private def service(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val receive: Pipe[IO, WebSocketFrame, Unit] = _.as(())

    HttpRoutes
      .of[IO] {
        case GET -> Root / "ts" => wsb.build(responseStream, receive)
      }
      .orNotFound
  }

  private val blaze = BlazeServerBuilder[IO]
    .bindHttp(8888, "0.0.0.0")
    .withMaxConnections(65536)
    .withConnectorPoolSize(connectorPoolSize)
    .withHttpWebSocketApp(wsb => service(wsb))
    .resource
    .useForever

  private val ember = EmberServerBuilder.default[IO]
    .withPort(port"8888")
    .withHost(host"0.0.0.0")
    .withMaxConnections(65536)
    .withHttpWebSocketApp(wsb => service(wsb))
    .withIdleTimeout(1.hour)
    .withShutdownTimeout(1.hour)
    .withRequestHeaderReceiveTimeout(1.hour)
    .build
    .useForever

  def run: IO[Unit] = blaze
}
