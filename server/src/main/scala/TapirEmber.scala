import cats.effect.{IO, IOApp}
import config.{WebServerConfig, TapirConfig}

object TapirEmber extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = WebServerConfig.ember.serverResource(TapirConfig.service).useForever
}
