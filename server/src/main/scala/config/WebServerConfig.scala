package config

import cats.effect.*
import com.comcast.ip4s.{Hostname, IpLiteralSyntax, Port}
import fs2.*
import fs2.concurrent.Topic
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.duration.*

/** Config shared among blaze/ember/zio-http http4s/tapir servers */
object WebServerConfig {

  val timestampPerUser = Stream.repeatEval(IO.realTime.map(ts => WebSocketFrame.Text(s"${ts.toMillis}")).delayBy(500.millis))
  def responseStreamFromTopic(topic: Topic[IO, WebSocketFrame]): Stream[IO, WebSocketFrame] = topic.subscribe(10)

  val port: Port = port"8888"
  val host: Hostname = host"0.0.0.0"
  val connectorPoolSize: Int = Math.max(2, Runtime.getRuntime.availableProcessors() / 4)
  private val maxConnections: Int = 65536

  def service(responseStream: Stream[IO, WebSocketFrame])(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val receive: Pipe[IO, WebSocketFrame, Unit] = _.as(())

    HttpRoutes
      .of[IO] {
        case GET -> Root / "ts" => wsb.build(responseStream, receive)
      }
      .orNotFound
  }

  /** Feeds the provided topic with a timestamp every 500ms */
  def tsService(topic: Topic[IO, WebSocketFrame]): IO[Nothing] = {
    IO.realTime.flatMap { ts =>
      val ts1Millis = ts.toMillis
      topic.publish1(WebSocketFrame.Text(s"${ts1Millis}")).map(_.fold(_ => throw new Exception("Topic closed"), _ => ts1Millis))
    }.flatMap { ts1Millis =>
      IO.realTime.map(ts2 =>
        if (math.abs(ts2.toMillis - ts1Millis) > 10) println(s"Delay: ${math.abs(ts2.toMillis - ts1Millis)}ms")
      )
    }.delayBy(500.millis).foreverM
  }

  object blaze {
    def serverResource(f: WebSocketBuilder2[IO] => HttpApp[IO]): Resource[IO, Server] = {
      BlazeServerBuilder[IO]
        .bindHttp(port.value, host.toString)
        .withMaxConnections(maxConnections)
        .withConnectorPoolSize(connectorPoolSize)
        .withHttpWebSocketApp(f)
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
        .withHttpWebSocketApp(f)
        .withIdleTimeout(idleTimeout)
        .withShutdownTimeout(shutdownTimeout)
        .withRequestHeaderReceiveTimeout(requestHeaderReceiveTimeout)
        .build
    }
  }


}
