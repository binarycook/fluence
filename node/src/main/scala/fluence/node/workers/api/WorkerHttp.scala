package fluence.node.workers.api

import cats.Parallel
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.effect.{Concurrent, Sync, Timer}
import fluence.log.{Log, LogFactory}
import fluence.node.workers.WorkersPorts
import fluence.worker.WorkersPool
import io.circe.syntax._
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import shapeless.{ops, HList}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.higherKinds

object WorkerHttp {

  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    p2p: ops.hlist.Selector[RS, WorkersPorts.P2pPort[F]]
  ): HttpRoutes[F] = {
    import dsl._

    object QueryWait extends OptionalQueryParamDecoderMatcher[Int]("wait")

    def status(appId: Long, timeout: FiniteDuration)(implicit log: Log[F]): F[Response[F]] =
      pool
        .getWorker(appId)
        .foldF(
          stage => HttpUtils.stageToResponse(appId, stage),
          w => w.status(timeout).flatMap(s ⇒ Ok(s.asJson.spaces2))
        )

    HttpRoutes.of {
      case GET -> Root / LongVar(appId) / "status" :? QueryWait(wait) ⇒
        LogFactory[F].init("http" -> "status", "app" -> appId.toString) >>= { implicit log =>
          // Fetches the worker's status, waiting no more than 10 seconds (if ?wait=$SECONDS is provided), or 1 second otherwise
          status(appId, wait.filter(_ < 10).fold(1.second)(_.seconds))
        }

      case GET -> Root / LongVar(appId) / "p2pPort" ⇒
        LogFactory[F].init("http" -> "p2pPort", "app" -> appId.toString) >>= { implicit log =>
          log.trace(s"Worker p2pPort") *>
            pool
              .getResources(appId)
              .map(_.select[WorkersPorts.P2pPort[F]])
              .semiflatMap(_.port)
              .value
              .flatMap {
                case Some(port) ⇒ Ok(port.toString)
                case None ⇒ NotFound()
              }
        }
    }
  }
}
