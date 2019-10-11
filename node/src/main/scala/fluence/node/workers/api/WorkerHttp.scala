package fluence.node.workers.api

import cats.{Applicative, Functor, Monad, Parallel}
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import fluence.log.{Log, LogFactory}
import fluence.worker.responder.WorkerResponder
import fluence.worker.{Worker, WorkersPool}
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request}
import shapeless.{ops, HList}

import scala.language.higherKinds

trait Handler[F[_]] {

  def handle(appId: Long, apiRequest: ApiRequest)(implicit log: Log[F]): EitherT[F, ApiErrorT, ApiResponse]
}

class MainHandler[F[_]: Monad, RS <: HList, CS <: HList](pool: WorkersPool[F, RS, CS])(
  implicit resp: ops.hlist.Selector[CS, WorkerResponder[F]]
) extends Handler[F] {

  private def handler(worker: Worker[F, CS])(
    implicit log: Log[F]
  ): ApiRequest ⇒ EitherT[F, ApiErrorT, ApiResponse] =
    (
      BasicHttp.handleRequest(worker) orElse SendAndWaitHttp.handleRequest(worker)
    ).lift(_).getOrElse(EitherT.leftT(ApiError("Handler not found")))

  override def handle(appId: Long, apiRequest: ApiRequest)(implicit log: Log[F]): EitherT[F, ApiErrorT, ApiResponse] = {
    pool
      .getWorker(appId)
      .leftMap(
        stage => WorkerNotResponding(appId, stage)
      )
      .flatMap(w => handler(w)(log)(apiRequest))

  }
}

object WorkerHttp {

  def readRequest[F[_]: Sync: Http4sDsl]: PartialFunction[Request[F], F[ApiRequest]] =
    BasicHttp.readRequest[F] orElse SendAndWaitHttp.readRequest[F]

  // ROUTES
  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel](
    handler: Handler[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): HttpRoutes[F] = {
    import dsl._
    import ApiError._

    HttpRoutes.of[F] {
      case req @ (GET | POST) -> Root / LongVar(appId) / _ ⇒
        // TODO move request caret there
        LogFactory[F].init("app" -> appId.toString) >>= { implicit log ⇒
          readRequest[F]
            .lift(req)
            .fold(
              BadRequest((ApiError("Cannot parse request"): ApiErrorT).asJson.noSpaces)
            )(
              apiReq ⇒
                apiReq.flatMap(
                  handler
                    .handle(appId, _)
                    .foldF(
                      err => HttpUtils.rpcErrorToResponse(err),
                      resp => Ok(resp.asJson.noSpaces)
                    )
              )
            )
        }
    }
  }
}
