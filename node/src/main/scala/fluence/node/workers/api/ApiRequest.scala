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

import fluence.bp.tx.TxCode
import fluence.effects.EffectError
import fluence.worker.WorkerStage
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.language.higherKinds

sealed trait ApiErrorT

object ApiError {
  implicit val apiErrorDecoder: Decoder[ApiErrorT] = deriveDecoder[ApiErrorT]
  implicit val apiErrorEncoder: Encoder[ApiErrorT] = deriveEncoder[ApiErrorT]
}
case class NoAppError(appId: Long, stage: WorkerStage) extends ApiErrorT
case class UnexpectedApiError(message: String) extends ApiErrorT
case class EffectApiError(message: String) extends ApiErrorT
case class ApiError(message: String) extends ApiErrorT

sealed trait ApiResponse

object ApiResponse {
  implicit val apiResponseDecoder: Decoder[ApiResponse] = deriveDecoder[ApiResponse]
  implicit val apiResponseEncoder: Encoder[ApiResponse] = deriveEncoder[ApiResponse]
}

sealed trait ApiRequest

object ApiRequest {
  implicit val apiRequestDecoder: Decoder[ApiRequest] = deriveDecoder[ApiRequest]
  implicit val apiRequestEncoder: Encoder[ApiRequest] = deriveEncoder[ApiRequest]
}

case class QueryResponse(result: String) extends ApiResponse
case class QueryRequest(path: String) extends ApiRequest

case class TxResponseNew(code: TxCode.Value, info: String, height: Option[Long] = None) extends ApiResponse
case class TxRequest(tx: Array[Byte]) extends ApiRequest

case class TxAwaitRequest(tx: Array[Byte]) extends ApiRequest

case class SubscribeResponse() extends ApiResponse
case class SubscribeRequest(subscriptionId: String, tx: Array[Byte]) extends ApiRequest

object NewApi {
// TODO should we need it
//  implicit def subscribeHandler[F[_]: Monad: Log](implicit RB: RepeatOnEveryBlock[F]): Handler[F, SubscribeRequest] = {
//    new Handler[F, SubscribeRequest] {
//      override def apply(req: SubscribeRequest): EitherT[F, ApiErrorT, SubscribeResponse] = {
//        val txData = Tx.Data(req.tx)
//        val key = SubscriptionKey.generate(req.subscriptionId, txData)
//        EitherT.liftF(RB.subscribe(key, txData).map(_ => SubscribeResponse()))
//      }
//    }
//  }
}
