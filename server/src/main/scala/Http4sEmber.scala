import cats.effect.*
import config.WebServerConfig

object Http4sEmber extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = WebServerConfig.ember.serverResource(WebServerConfig.service(WebServerConfig.timestampPerUser)).useForever
}
