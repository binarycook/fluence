package fluence.node.workers.api

import cats.Parallel
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import fluence.log.{Log, LogFactory}
import fluence.node.workers.WorkersPorts
import fluence.worker.responder.WorkerResponder
import fluence.worker.{Worker, WorkersPool}
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request}
import shapeless.{ops, HList}

import scala.language.higherKinds

object WorkerHttp {

  def readRequest[F[_]: Sync: Http4sDsl]: PartialFunction[Request[F], F[ApiRequest]] =
    BasicHttp.readRequest[F] orElse SendAndWaitHttp.readRequest[F]

  // ROUTES
  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    p2p: ops.hlist.Selector[RS, WorkersPorts.P2pPort[F]],
    resp: ops.hlist.Selector[CS, WorkerResponder[F]]
  ): HttpRoutes[F] = {
    import dsl._

    def handle(worker: Worker[F, CS])(implicit log: Log[F]): ApiRequest ⇒ EitherT[F, ApiErrorT, ApiResponse] =
      (
        BasicHttp.handleRequest(worker) orElse SendAndWaitHttp.handleRequest(worker)
      ).lift(_).getOrElse(EitherT.leftT(ApiError("Handler not found")))

    HttpRoutes.of[F] {
      case req @ (GET | POST) -> Root / LongVar(appId) / _ ⇒
        // TODO move request caret there
        LogFactory[F].init("app" -> appId.toString) >>= { implicit log ⇒
          readRequest[F]
            .lift(req)
            .fold(
              BadRequest("Cannot parse request")
            )(
              apiReq ⇒
                pool
                  .getWorker(appId)
                  .foldF(
                    HttpUtils.stageToResponse(appId, _),
                    w ⇒
                      (EitherT.right[ApiErrorT](apiReq) >>= handle(w)).foldF(
                        HttpUtils.rpcErrorToResponse(_),
                        resp ⇒ Ok(resp.asJson.noSpaces)
                    )
                )
            )
        }
    }
  }
}
