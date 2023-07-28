import cats.effect.*
import config.WebServerConfig

object Http4sEmber extends IOApp.Simple {
  override protected def computeWorkerThreadCount: Int = WebServerConfig.mainPoolSize

  def run: IO[Unit] = WebServerConfig.ember.serverResource(WebServerConfig.service).useForever
}
