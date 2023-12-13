package gatling

import io.gatling.core.Predef.*
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.http.Predef.*
import io.gatling.http.action.ws.WsSendTextFrameBuilder
import org.HdrHistogram.ConcurrentHistogram

import scala.concurrent.duration.DurationInt

class SimpleWsServerSimulation extends Simulation {

  import SimpleWsServerSimulation.*

  private val wsPubHttpProtocol = http.wsBaseUrl(config.wsServerUri)

  private val numOfMessagesPerUser = 60 * 10 // 60 seconds with 10 msg/sec

  def subscribe(name: String): WsSendTextFrameBuilder = {
    val req = ws(name).sendText("0")
    val check = ws.checkTextMessage(name).check(
      bodyString.transform { ts =>
        hist.recordValue(Math.max(System.currentTimeMillis() - ts.toLong, 0))
      }
    )

    Range.inclusive(1, numOfMessagesPerUser).foldLeft(req)((acc, _) => acc.await(1.second)(check))
  }

  private val warmup = scenario("WS warmup")
    .exec(ws("Warmup Connect WS").connect("/ts"))
    .exec(subscribe("Warmup Subscribe"))
    .exec(ws("Warmup Close WS").close)
    .exec(pause(40.seconds))
    .exec({
      session =>
        hist.reset()
        session
    })
    .inject(config.injectionPolicy)

  private val measurement = scenario("WS measurement")
    .exec(ws("Connect WS").connect("/ts"))
    .exec(subscribe("Subscribe"))
    .exec(ws("Close WS").close)
    .inject(config.injectionPolicy)

  setUp(
    warmup.andThen(measurement)
  ).protocols(wsPubHttpProtocol)
}


object SimpleWsServerSimulation {
  private val hist = new ConcurrentHistogram(1L, 10000L, 3)

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit =
      hist.outputPercentileDistribution(System.out, 1.0)
  })

  object config {
    val numberOfUsers = 10000

    val wsServerUri = "ws://172.16.255.3:8888"

    val injectionPolicy: OpenInjectionStep = rampUsers(numberOfUsers).during(30.seconds)
  }
}
