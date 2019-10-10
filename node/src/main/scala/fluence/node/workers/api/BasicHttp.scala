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

import cats.{Applicative, Functor, Parallel}
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import fluence.bp.api.BlockProducer
import fluence.log.{Log, LogFactory}
import fluence.statemachine.api.StateMachine
import fluence.worker.{Worker, WorkersPool}
import io.circe.syntax._
import org.http4s.{HttpRoutes, Request}
import org.http4s.dsl.Http4sDsl
import shapeless._

import scala.language.higherKinds

object BasicHttp {

  def readRequest[F[_]: Sync](implicit dsl: Http4sDsl[F]): PartialFunction[Request[F], F[ApiRequest]] = {
    import dsl._
    object QueryPath extends QueryParamDecoderMatcher[String]("path")

    {
      case GET -> Root / _ / "query" :? QueryPath(path) ⇒
        Applicative[F].pure(QueryRequest(path))

      case req @ POST -> Root / _ / "tx" ⇒
        req.as[Array[Byte]].map(TxRequest)
    }
  }

  // TODO move it away
  def handleRequest[F[_]: Functor: Log](
    worker: Worker[F, _ <: HList]
  ): PartialFunction[ApiRequest, EitherT[F, ApiErrorT, ApiResponse]] = {
    case QueryRequest(path) ⇒
      worker.machine
        .query(path)
        .bimap(
          e => EffectApiError("", e): ApiErrorT,
          r => QueryResponse(new String(r.result))
        )

    case TxRequest(tx) ⇒
      worker.producer
        .sendTx(tx)
        .leftMap(e => EffectApiError("", e): ApiErrorT)
        .map(r => TxResponseNew(r.code, r.info, r.height))
  }

}
