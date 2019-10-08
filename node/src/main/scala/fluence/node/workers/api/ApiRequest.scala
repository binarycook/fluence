/*
 * Copyright 2018 Fluence Labs Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fluence.node.workers.api

import cats.effect.Sync
import fluence.bp.tx.TxCode
import fluence.effects.EffectError

sealed trait ApiError
case class UnexpectedApiError(message: String, throwable: Option[Throwable] = None) extends ApiError {}
case class EffectApiError(message: String, effectError: EffectError) extends ApiError {}

sealed trait ApiResponse
sealed trait ApiRequest {
  self ⇒
  type Resp <: ApiResponse
}

object ApiRequest {
  type Aux[T <: ApiRequest, R <: ApiResponse] = T { type Resp = R }

  implicit class HandleOps[T <: ApiRequest, R <: ApiResponse](req: Aux[T, R]) {

    def handle[F[_]]()(implicit handler: NewApi.Handler[F, T]): F[R] =
      handler(req)
  }
}

case class QueryResponse(result: String) extends ApiResponse
case class QueryRequest(path: String, id: Option[String], data: Option[String]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class TxResponse(code: TxCode.Value, info: String, height: Option[Long] = None) extends ApiResponse
case class TxRequest(tx: Array[Byte]) extends ApiRequest {
  override type Resp = TxResponse
}

case class TxAwaitRequest(tx: Array[Byte]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class WebsocketRequest[T <: ApiRequest](id: String, request: T)
case class WebsocketResponse[T <: ApiResponse](id: String, response: T)

object NewApi {

  abstract class Handler[F[_], T <: ApiRequest] {
    def apply(req: T): F[req.Resp]
  }

  implicit def queryHandler[F[_]: Sync]: Handler[F, QueryRequest] = new Handler[F, QueryRequest] {
    override def apply(req: QueryRequest): F[QueryResponse] =
      Sync[F].delay(QueryResponse(req.toString))
  }

  QueryRequest("???", None, None).handle[cats.effect.SyncIO].unsafeRunSync().result

}
