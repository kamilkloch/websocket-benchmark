import cats.effect.{IO, IOApp}
import com.comcast.ip4s.*
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.duration.DurationInt

object TapirWebSocketServer extends IOApp.Simple {
  private val wsEndpoint = endpoint.get
    .in("ts")
    .out(webSocketBody[Long, CodecFormat.TextPlain, Long, CodecFormat.TextPlain](Fs2Streams[IO]))

  private val responseStream = Stream.repeatEval(IO.realTime.map(_.toMillis).delayBy(500.millis))
  private val wsRoutes = Http4sServerInterpreter[IO]()
    .toWebSocketRoutes(wsEndpoint.serverLogicSuccess(_ => IO.pure((_: Stream[IO, Long]) => responseStream)))
  private val connectorPoolSize = Math.max(2, Runtime.getRuntime.availableProcessors() / 4)

  override protected def computeWorkerThreadCount: Int =
    Math.max(2, super.computeWorkerThreadCount / 2)

  private val blaze = BlazeServerBuilder[IO]
    .bindHttp(8888, "0.0.0.0")
    .withMaxConnections(65536)
    .withConnectorPoolSize(connectorPoolSize)
    .withHttpWebSocketApp(wsb => Router("/" -> wsRoutes(wsb)).orNotFound)
    .resource
    .useForever

  private val ember = EmberServerBuilder.default[IO]
    .withPort(port"8888")
    .withHost(host"0.0.0.0")
    .withMaxConnections(65536)
    .withHttpWebSocketApp(wsb => Router("/" -> wsRoutes(wsb)).orNotFound)
    .build
    .useForever

  def run: IO[Unit] = blaze
}
