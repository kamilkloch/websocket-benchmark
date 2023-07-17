import cats.effect.*
import config.Http4sConfig

object Http4sBlaze extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = Http4sConfig.blaze.serverResource(Http4sConfig.service).useForever
}
