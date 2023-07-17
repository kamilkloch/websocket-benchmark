package gatling

import io.gatling.core.Predef.*
import io.gatling.http.Predef.*
import io.gatling.http.action.ws.WsSendTextFrameBuilder
import org.HdrHistogram.ConcurrentHistogram

import scala.concurrent.duration.DurationInt

class SimpleWsServerSimulation extends Simulation {

  import SimpleWsServerSimulation.*

  private val wsPubHttpProtocol = http.wsBaseUrl(config.wsServerUri)

  private val numOfMessagesPerUser = 120

  def subscribe(channel: String): WsSendTextFrameBuilder = {
    var req = ws(s"Subscribe $channel").sendText(channel)
    val check = ws.checkTextMessage("checkFrame").check(
      bodyString.transform { ts =>
        hist.recordValue(Math.max(System.currentTimeMillis() - ts.toLong, 0))
      }
    )
    for (_ <- 0 until numOfMessagesPerUser)
      req = req.await(1.second)(check)
    req
  }

  private val subscribeMD = subscribe("0")

  private val scn = scenario("WS subscribe")
    .exec(ws("Connect WS").connect("/ts"))
    .repeat(1)(exec(subscribeMD))
    .exec(ws("Close WS").close)

  setUp(
    scn.inject(config.injectionPolicy)
  ).protocols(wsPubHttpProtocol)
}


object SimpleWsServerSimulation {
  private val hist = new ConcurrentHistogram(1L, 1_000_000L, 3)

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit =
      hist.outputPercentileDistribution(System.out, 1.0)
  })

  object config {
    val numberOfUsers = 10000

    val wsServerUri = "ws://127.0.0.1:8888"

    val injectionPolicy = rampUsers(numberOfUsers).during(30.seconds)
  }
}
