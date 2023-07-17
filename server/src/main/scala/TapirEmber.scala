import cats.effect.{IO, IOApp}
import config.{Http4sConfig, TapirConfig}

object TapirEmber extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = Http4sConfig.ember.serverResource(TapirConfig.service).useForever
}
