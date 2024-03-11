import cats.effect.{IO, IOApp}
import config.{WebServerConfig, TapirConfig}

object TapirEmber extends IOApp.Simple {
  def run: IO[Unit] = WebServerConfig.ember.serverResource(TapirConfig.service).useForever
}
