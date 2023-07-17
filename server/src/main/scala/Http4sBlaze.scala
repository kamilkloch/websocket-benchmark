import cats.effect.*
import config.WebServerConfig

object Http4sBlaze extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = WebServerConfig.blaze.serverResource(WebServerConfig.service).useForever
}
