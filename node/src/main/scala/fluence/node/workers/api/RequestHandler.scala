package fluence.node.workers.api

import cats.Monad
import cats.data.EitherT
import fluence.log.Log
import fluence.worker.WorkersPool
import fluence.worker.responder.WorkerResponder
import shapeless.{ops, HList}

import scala.language.higherKinds

trait RequestHandler[F[_]] {
  def handle(appId: Long, req: ApiRequest)(implicit log: Log[F]): EitherT[F, ApiErrorT, ApiResponse]
}

class RequestHandlerImpl[F[_]: Monad, R, CS <: HList](workersPool: WorkersPool[F, R, CS])(
  implicit resp: ops.hlist.Selector[CS, WorkerResponder[F]]
) extends RequestHandler[F] {

  def handle(appId: Long, req: ApiRequest)(implicit log: Log[F]): EitherT[F, ApiErrorT, ApiResponse] = {
    workersPool.getWorker(appId).leftMap(s => NoAppError(appId, s): ApiErrorT).flatMap { worker =>
      val handleFunc = (
        BasicHttp.handleRequest(worker) orElse SendAndWaitHttp.handleRequest(worker)
      ).lift(_).getOrElse(EitherT.leftT(ApiError("Handler not found")))
      handleFunc(req)
    }
  }
}
