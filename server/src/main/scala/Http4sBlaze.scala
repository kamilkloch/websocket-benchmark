import cats.effect.*
import config.WebServerConfig

object Http4sBlaze extends IOApp.Simple {
  override protected def computeWorkerThreadCount: Int = WebServerConfig.mainPoolSize

  def run: IO[Unit] = WebServerConfig.blaze.serverResource(WebServerConfig.service).useForever
}
