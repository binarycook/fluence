package fluence.node.workers.api

import cats.Monad
import cats.syntax.apply._
import fluence.effects.tendermint.rpc.http.{
  RpcBlockParsingFailed,
  RpcBodyMalformed,
  RpcCallError,
  RpcError,
  RpcHttpError,
  RpcRequestFailed
}
import fluence.log.Log
import fluence.worker.WorkerStage
import org.http4s.Response
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object HttpUtils {

  /**
   * Encodes errors to HTTP format.
   *
   */
  def rpcErrorToResponse[F[_]: Monad](
    error: ApiErrorT
  )(implicit log: Log[F], dsl: Http4sDsl[F]): F[Response[F]] = {
    import dsl._
    error match {
      case UnexpectedApiError(message, err) =>
        log.warn(s"RPC request failed", err) *>
          InternalServerError(err.getMessage)
      case EffectApiError(message, err) =>
        log.warn(s"RPC request failed: $message", err) *>
          InternalServerError(err.getMessage)
      case ApiError(message) =>
        log.warn(s"RPC request failed: $message") *>
          InternalServerError(message)
      /*case RpcRequestFailed(err) ⇒
        log.warn(s"RPC request failed", err) *>
          InternalServerError(err.getMessage)

      case err: RpcHttpError ⇒
        log.warn(s"RPC request errored", err) *>
          InternalServerError(err.error)

      case err: RpcCallError =>
        log.warn(s"RPC call resulted in error", err) *>
          // TODO: is it OK to return BadRequest here?
          BadRequest(err.getMessage)

      case err: RpcBodyMalformed ⇒
        log.warn(s"RPC body malformed: $err", err) *>
          BadRequest(err.getMessage)

      case err: RpcBlockParsingFailed =>
        log.warn(s"RPC $err", err) *>
          InternalServerError(err.getMessage)*/
    }
  }

  def stageToResponse[F[_]: Monad](
    appId: Long,
    workerStage: WorkerStage
  )(implicit dsl: Http4sDsl[F], log: Log[F]): F[Response[F]] = {
    import dsl._
    val msg = s"Worker for $appId can't serve RPC: it is in stage $workerStage"
    log.debug(msg) *>
      NotFound(msg)
  }
}
