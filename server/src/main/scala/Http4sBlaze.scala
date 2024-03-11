import cats.effect.*
import config.WebServerConfig

object Http4sBlaze extends IOApp.Simple {
  def run: IO[Unit] = WebServerConfig.blaze.serverResource(WebServerConfig.service).useForever
}
