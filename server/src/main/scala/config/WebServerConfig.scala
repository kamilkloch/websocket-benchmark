package config

import cats.effect.*
import com.comcast.ip4s.{Hostname, IpLiteralSyntax, Port}
import fs2.*
import fs2.io.net.SocketOption
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import java.net.StandardSocketOptions
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.*

/** Config shared among blaze/ember/zio-http http4s/tapir servers */
object WebServerConfig {
  val responseStream: Stream[IO, WebSocketFrame.Text] =
    Stream.eval(IO(new AtomicLong(0))).flatMap { ts =>
      Stream.fixedRate[IO](100.millis, dampen = false).map(_ => WebSocketFrame.Text(ts.updateAndGet {
        case 0 => System.currentTimeMillis()
        case x => x + 100
      }.toString))
    }
  val port: Port = port"8888"
  val host: Hostname = host"0.0.0.0"
  private val maxConnections: Int = 65536

  def service(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}

    import dsl.*

    val receive: Pipe[IO, WebSocketFrame, Unit] = _.as(())

    HttpRoutes.of[IO] {
        case GET -> Root / "ts" => wsb/*.withDefragment(false).withFilterPingPongs(false)*/.build(responseStream, receive)
      }
      .orNotFound
  }

  object blaze {
    def serverResource(f: WebSocketBuilder2[IO] => HttpApp[IO]): Resource[IO, Server] = {
      BlazeServerBuilder[IO]
        .bindHttp(port.value, host.toString)
        .withMaxConnections(maxConnections)
        .withHttpWebSocketApp(f)
/*
        .withDefaultTcpNoDelay
        .withDefaultSocketReuseAddress
*/
        .resource
    }
  }

  object ember {
    def serverResource(f: WebSocketBuilder2[IO] => HttpApp[IO]): Resource[IO, Server] = {
      EmberServerBuilder.default[IO]
        .withPort(port)
        .withHost(host)
        .withMaxConnections(maxConnections)
        .withHttpWebSocketApp(f)
        .withIdleTimeout(1.hour)
/*
        .withAdditionalSocketOptions(List(
          SocketOption(StandardSocketOptions.TCP_NODELAY, java.lang.Boolean.TRUE),
          SocketOption(StandardSocketOptions.SO_REUSEADDR, java.lang.Boolean.TRUE)))
*/
        .build
    }
  }
}
