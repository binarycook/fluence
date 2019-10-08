package fluence.node.workers.api

import fluence.bp.tx.TxCode
import fluence.effects.EffectError

sealed trait ApiError
case class UnexpectedApiError(message: String, throwable: Option[Throwable] = None) extends ApiError {}
case class EffectApiError(message: String, effectError: EffectError) extends ApiError {}

sealed trait ApiResponse
sealed trait ApiRequest {
  type Resp <: ApiResponse
}

case class QueryResponse(result: String) extends ApiResponse
case class QueryRequest(path: String, id: Option[String], data: Option[String]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class TxResponse(code: TxCode.Value, info: String, height: Option[Long] = None) extends ApiResponse
case class TxRequest(tx: Array[Byte]) {
  override type Resp = TxResponse
}

case class TxAwaitRequest(tx: Array[Byte]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class WebsocketRequest[T <: ApiRequest](id: String, request: T)
case class WebsocketResponse[T <: ApiResponse](id: String, response: T)
