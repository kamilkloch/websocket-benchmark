import cats.effect.{IO, IOApp}
import config.{Http4sConfig, TapirConfig}

object TapirBlaze extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = Http4sConfig.blaze.serverResource(TapirConfig.service).useForever
}
