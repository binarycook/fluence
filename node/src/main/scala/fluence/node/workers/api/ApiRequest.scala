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

import cats.data.EitherT
import cats.effect.{ContextShift, IO, Sync, Timer}
import fluence.bp.api.BlockProducer
import fluence.bp.tx.{Tx, TxCode}
import fluence.effects.EffectError
import fluence.log.{Log, LogFactory}
import fluence.node.workers.api.websocket.WebsocketRequests.TxWaitRequest
import fluence.statemachine.api.data.StateMachineStatus
import fluence.statemachine.api.query.QueryCode
import fluence.statemachine.api.StateMachine
import fluence.worker.responder.{SendAndWait, WorkerResponder}
import fluence.worker.responder.resp.{AwaitedResponse, OkResponse, TxAwaitError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

sealed trait ApiErrorT
case class UnexpectedApiErrorT(message: String, throwable: Option[Throwable] = None) extends ApiErrorT
case class EffectApiErrorT(message: String, effectError: EffectError) extends ApiErrorT
case class ApiError(message: String) extends ApiErrorT

sealed trait ApiResponse
sealed trait ApiRequest {
  self ⇒
  type Resp <: ApiResponse
}

object ApiRequest {
  type Aux[T <: ApiRequest, R <: ApiResponse] = T { type Resp = R }

  implicit class HandleOps[T <: ApiRequest, R <: ApiResponse](req: Aux[T, R]) {

    def handle[F[_]]()(implicit handler: NewApi.Handler[F, T]): EitherT[F, ApiErrorT, R] =
      handler(req)
  }
}

case class QueryResponse(result: String) extends ApiResponse
case class QueryRequest(path: String, id: Option[String], data: Option[String]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class TxResponseNew(code: TxCode.Value, info: String, height: Option[Long] = None) extends ApiResponse
case class TxRequest(tx: Array[Byte]) extends ApiRequest {
  override type Resp = TxResponseNew
}

case class TxAwaitRequest(tx: Array[Byte]) extends ApiRequest {
  override type Resp = QueryResponse
}

case class WebsocketRequest[T <: ApiRequest](id: String, request: T)
case class WebsocketResponse[T <: ApiResponse](id: String, response: T)

object NewApi {

  abstract class Handler[F[_], T <: ApiRequest] {
    def apply(req: T): EitherT[F, ApiErrorT, req.Resp]
  }

  implicit def queryHandler[F[_]: Sync: Log](implicit SM: StateMachine[F]): Handler[F, QueryRequest] =
    new Handler[F, QueryRequest] {
      override def apply(req: QueryRequest): EitherT[F, ApiErrorT, QueryResponse] =
        SM.query(req.path).map(r => QueryResponse(new String(r.result))).leftMap(e => EffectApiErrorT("", e): ApiErrorT)
    }

  implicit def txHandler[F[_]: Sync: Log](implicit SW: SendAndWait[F]): Handler[F, TxAwaitRequest] =
    new Handler[F, TxAwaitRequest] {
      override def apply(req: TxAwaitRequest): EitherT[F, ApiErrorT, QueryResponse] =
        SW.sendTxAwaitResponse(req.tx)
          .map {
            case OkResponse(id, response) => QueryResponse(response)
            case _                        => QueryResponse("unexpected result")
          }
          .leftMap(e => ApiError(e.msg): ApiErrorT)
    }

}

object TestRun extends App {

  import NewApi._
  //producer: BlockProducer[F]
  implicit private val ioTimer: Timer[IO] = IO.timer(global)
  implicit private val ioShift: ContextShift[IO] = IO.contextShift(global)
  implicit private val logFactory = LogFactory.forPrintln[IO](level = Log.Error)
  implicit private val log = logFactory.init("ResponseSubscriberSpec", level = Log.Off).unsafeRunSync()

  implicit val stateMachine: StateMachine[IO] = new StateMachine.ReadOnly[IO] {
    override def query(
      path: String
    )(implicit log: Log[IO]): EitherT[IO, EffectError, fluence.statemachine.api.query.QueryResponse] =
      EitherT.fromEither(Right(fluence.statemachine.api.query.QueryResponse(1, path.getBytes(), QueryCode.Ok, "")))
    override def status()(implicit log: Log[IO]): EitherT[IO, EffectError, StateMachineStatus] = ???
  }

  implicit val responder = new SendAndWait[IO]() {
    override def sendTxAwaitResponse(tx: Array[Byte])(
      implicit log: Log[IO]
    ): EitherT[IO, TxAwaitError, AwaitedResponse] =
      EitherT.fromEither(Right(OkResponse(Tx.Head("session", 1), "response: " + new String(tx))))
  }

  val res = QueryRequest("???", None, None).handle[IO].value.unsafeRunSync()
  println(res)

  val res2 = TxAwaitRequest("some-tx".getBytes()).handle[IO].value.unsafeRunSync()
  println(res2)
}
