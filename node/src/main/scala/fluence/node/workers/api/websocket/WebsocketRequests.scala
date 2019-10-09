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

package fluence.node.workers.api.websocket

import fluence.node.workers.api.{ApiRequest, ApiResponse}
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

object WebsocketRequests {

//  case class WebsocketRequest[T <: ApiRequest](id: String, request: T)
//  case class WebsocketResponse[T <: ApiResponse](id: String, response: T)

  sealed trait WebsocketRequest {
    def requestId: String
  }
  case class QueryRequest(path: String, data: Option[String], id: Option[String], requestId: String)
      extends WebsocketRequest
  case class TxRequest(tx: String, requestId: String) extends WebsocketRequest
  case class TxWaitRequest(tx: String, requestId: String) extends WebsocketRequest
  case class SubscribeRequest(requestId: String, subscriptionId: String, tx: String) extends WebsocketRequest
  case class UnsubscribeRequest(requestId: String, subscriptionId: String, tx: String) extends WebsocketRequest

  object WebsocketRequest {
    implicit val conf: Configuration =
      Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit val websocketRequestDecoder: Decoder[WebsocketRequest] = deriveConfiguredDecoder[WebsocketRequest]
    implicit val websocketRequestEncoder: Encoder[WebsocketRequest] = deriveConfiguredEncoder[WebsocketRequest]
  }
}
