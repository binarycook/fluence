package fluence.node.workers.api

import cats.syntax.flatMap._
import cats.syntax.apply._
import io.circe.syntax._
import cats.{Monad, Parallel}
import cats.effect.{Concurrent, Sync, Timer}
import fluence.bp.tx.Tx
import fluence.effects.tendermint.rpc.http.{
  RpcBlockParsingFailed,
  RpcBodyMalformed,
  RpcCallError,
  RpcError,
  RpcHttpError,
  RpcRequestFailed
}
import fluence.log.{Log, LogFactory}
import fluence.worker.WorkersPool
import fluence.worker.responder.WorkerResponder
import fluence.worker.responder.resp.{
  OkResponse,
  PendingResponse,
  RpcErrorResponse,
  RpcTxAwaitError,
  TimedOutResponse,
  TxInvalidError,
  TxParsingError
}
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import shapeless.{ops, HList}

import scala.language.higherKinds

object SendAndWaitHttp {

  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    p2p: ops.hlist.Selector[CS, WorkerResponder[F]]
  ): HttpRoutes[F] = {
    import dsl._
    import ApiRequest._

    HttpRoutes.of[F] {
      case req @ POST -> Root / LongVar(appId) / "txWaitResponse" ⇒
        LogFactory[F].init("http" -> "txAwaitResponse", "app" -> appId.toString) >>= { log =>
          req.decode[Array[Byte]] { tx ⇒
            val txHead = Tx.splitTx(tx).fold("not parsed")(_._1)
            log.scope("tx.head" -> txHead) { implicit log =>
              log.trace("requested") >> pool
                .getCompanion[WorkerResponder[F]](appId)
                .foldF(
                  stage => HttpUtils.stageToResponse(appId, stage),
                  wr => {
                    implicit val sw = wr.sendAndWait
                    TxAwaitRequest(tx)
                      .handle[F]
                      .foldF(err => HttpUtils.rpcErrorToResponse(err), r => Ok((r: ApiResponse).asJson.noSpaces))
                  }
                )
                .flatTap(r => log.trace(s"response: $r"))
            }
          }
        }
    }
  }
}
