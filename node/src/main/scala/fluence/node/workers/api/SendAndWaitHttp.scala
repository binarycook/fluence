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

import cats.Functor
import cats.data.EitherT
import cats.syntax.functor._
import cats.effect.Sync
import fluence.bp.tx.Tx
import fluence.log.Log
import fluence.worker.Worker
import fluence.worker.responder.WorkerResponder
import org.http4s.Request
import org.http4s.dsl.Http4sDsl
import shapeless._

import scala.language.higherKinds

object SendAndWaitHttp {

  def readRequest[F[_]: Sync](implicit dsl: Http4sDsl[F]): PartialFunction[Request[F], F[ApiRequest]] = {
    import dsl._

    {
      case req @ POST -> Root / "txWaitResponse" ⇒
        req.as[Array[Byte]].map(TxAwaitRequest)
    }
  }

  // TODO move it away
  def handleRequest[F[_]: Functor, CS <: HList](
    worker: Worker[F, CS]
  )(
    implicit wr: ops.hlist.Selector[CS, WorkerResponder[F]],
    log: Log[F]
  ): PartialFunction[ApiRequest, EitherT[F, ApiErrorT, ApiResponse]] = {
    case TxAwaitRequest(tx) ⇒
      val txHead = Tx.splitTx(tx).fold("not parsed")(_._1)
      log.scope("tx.head" -> txHead) { implicit log =>
        worker
          .companion[WorkerResponder[F]]
          .sendAndWait
          .sendTxAwaitResponse(tx)
          .bimap(
            err ⇒ ApiError(err.msg),
            res ⇒ ???
          )
      }
  }

}
