import cats.effect.*
import config.WebServerConfig

object Http4sEmber extends IOApp.Simple {
  def run: IO[Unit] = WebServerConfig.ember.serverResource(WebServerConfig.service).useForever
}
