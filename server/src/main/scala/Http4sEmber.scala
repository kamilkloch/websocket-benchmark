import cats.effect.*
import config.Http4sConfig

object Http4sEmber extends IOApp.Simple {

  override protected def computeWorkerThreadCount: Int = Math.max(2, super.computeWorkerThreadCount / 2)

  def run: IO[Unit] = Http4sConfig.ember.serverResource(Http4sConfig.service).useForever
}
