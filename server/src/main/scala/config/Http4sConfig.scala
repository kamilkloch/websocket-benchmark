package config

import cats.effect.*
import com.comcast.ip4s.{Hostname, IpLiteralSyntax, Port}
import fs2.*
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.duration.*

/** Config shared among blaze/ember http4s/tapir servers */
object Http4sConfig {

  private val responseStream = Stream.repeatEval(IO.realTime.map(ts => WebSocketFrame.Text(s"${ts.toMillis}")).delayBy(500.millis))

  private val port: Port = port"8888"
  private val host: Hostname = host"0.0.0.0"
  private val maxConnections: Int = 65536

  def service(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val receive: Pipe[IO, WebSocketFrame, Unit] = _.as(())

    HttpRoutes
      .of[IO] {
        case GET -> Root / "ts" => wsb.build(responseStream, receive)
      }
      .orNotFound
  }

  object blaze {
    private def connectorPoolSize: Int = Math.max(2, Runtime.getRuntime.availableProcessors() / 4)

    def serverResource(f: WebSocketBuilder2[IO] => HttpApp[IO]): Resource[IO, Server] = {
      BlazeServerBuilder[IO]
        .bindHttp(port.value, host.toString)
        .withMaxConnections(maxConnections)
        .withConnectorPoolSize(connectorPoolSize)
        .withHttpWebSocketApp(service)
        .resource
    }
  }

  object ember {
    private val idleTimeout: FiniteDuration = 1.hour
    private val shutdownTimeout: FiniteDuration = 1.hour
    private val requestHeaderReceiveTimeout: FiniteDuration = 1.hour

    def serverResource(f: WebSocketBuilder2[IO] => HttpApp[IO]): Resource[IO, Server] = {
      EmberServerBuilder.default[IO]
        .withPort(port)
        .withHost(host)
        .withMaxConnections(maxConnections)
        .withHttpWebSocketApp(service)
        .withIdleTimeout(idleTimeout)
        .withShutdownTimeout(shutdownTimeout)
        .withRequestHeaderReceiveTimeout(requestHeaderReceiveTimeout)
        .build
    }
  }

}
