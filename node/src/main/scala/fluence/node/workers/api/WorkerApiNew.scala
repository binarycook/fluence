package fluence.node.workers.api

import cats.Applicative
import cats.data.EitherT
import fluence.bp.api.BlockProducer
import fluence.effects.tendermint.rpc.http.{RpcError, RpcRequestFailed}
import fluence.log.Log
import fluence.node.workers.WorkersPorts
import fluence.node.workers.api.websocket.WebsocketRequests.TxWaitRequest
import fluence.statemachine.api.StateMachine
import fluence.worker.responder.WorkerResponder

import scala.language.higherKinds

trait WorkerApiNew[F[_]] {
  def handle[T <: ApiRequest](req: T): EitherT[F, ApiError, req.Resp]
}

class WorkerApiNewImpl[F[_]: Applicative](producer: BlockProducer[F],
                                          responder: WorkerResponder[F],
                                          machine: StateMachine[F],
                                          p2pPort: WorkersPorts.P2pPort[F])(implicit log: Log[F])
    extends WorkerApiNew[F] {
  override def handle[T <: ApiRequest, E <: ApiError](req: T): EitherT[F, E, req.Resp] = {
    req match {
      case r: TxRequest =>
        EitherT.leftT[req.Resp](UnexpectedApiError("unimplemented TxRequest"): ApiError)
      case TxWaitRequest => EitherT.leftT[req.Resp](UnexpectedApiError("unimplemented TxWaitRequest"): ApiError)
      case r: QueryRequest =>
        machine
          .query(r.path)
          .leftMap(e ⇒ EffectApiError("", RpcRequestFailed(e)))
    }
  }
}
