package config

import cats.effect.*
import fs2.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.{CodecFormat, endpoint, webSocketBody}

import scala.concurrent.duration.*

/** Config shared among blaze/ember tapir servers */
object TapirConfig {
  private val wsEndpoint = endpoint.get
    .in("ts")
    .out(webSocketBody[Long, CodecFormat.TextPlain, Long, CodecFormat.TextPlain](Fs2Streams[IO]))
  private val responseStream = Stream.fixedRate[IO](100.millis, dampen = false).evalMap(_ => IO.realTime.map(_.toMillis))
  private val serverOptions = Http4sServerOptions
    .customiseInterceptors[IO]
    .serverLog(None)
    .options
  private val wsRoutes = Http4sServerInterpreter[IO](serverOptions)
    .toWebSocketRoutes(wsEndpoint.serverLogicSuccess(_ => IO.pure { (in: Stream[IO, Long]) =>
      responseStream.concurrently(in.as(()))
    }))

  def service(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = Router("/" -> wsRoutes(wsb)).orNotFound
}
