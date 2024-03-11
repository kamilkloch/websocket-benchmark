import cats.effect.{IO, IOApp}
import config.{WebServerConfig, TapirConfig}

object TapirBlaze extends IOApp.Simple {
  def run: IO[Unit] = WebServerConfig.blaze.serverResource(TapirConfig.service).useForever
}
