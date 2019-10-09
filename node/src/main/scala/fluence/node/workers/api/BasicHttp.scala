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

import cats.Parallel
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import fluence.bp.api.BlockProducer
import fluence.log.LogFactory
import fluence.statemachine.api.StateMachine
import fluence.worker.WorkersPool
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import shapeless._

import scala.language.higherKinds

object BasicHttp {

  /**
   * Routes for Workers API.
   *
   * @param pool Workers pool to get workers from
   * @param dsl Http4s DSL to build routes with
   */
  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    wr: ops.hlist.Selector[CS, StateMachine[F]],
    bp: ops.hlist.Selector[CS, BlockProducer[F]]
  ): HttpRoutes[F] = {
    import ApiRequest._
    import dsl._

    object QueryPath extends QueryParamDecoderMatcher[String]("path")

    // Routes comes there
    HttpRoutes.of {
      case GET -> Root / LongVar(appId) / "query" :? QueryPath(path) ⇒
        LogFactory[F].init("http" -> "query", "app" -> appId.toString) >>= { implicit log =>
          pool
            .getCompanion[StateMachine[F]](appId)
            .foldF(
              stage => HttpUtils.stageToResponse(appId, stage),
              implicit sm =>
                QueryRequest(path)
                  .handle[F]
                  .foldF(
                    err => HttpUtils.rpcErrorToResponse(err),
                    r => Ok((r: ApiResponse).asJson.noSpaces)
                )
            )

        }

      case req @ POST -> Root / LongVar(appId) / "tx" ⇒
        LogFactory[F].init("http" -> "tx", "app" -> appId.toString) >>= { implicit log =>
          req.decode[Array[Byte]] { tx ⇒
            log.debug(s"tx.head: ${tx.takeWhile(_ != '\n')}") >>
              pool
                .getCompanion[BlockProducer[F]](appId)
                .foldF(
                  stage => HttpUtils.stageToResponse(appId, stage),
                  implicit bp =>
                    TxRequest(tx)
                      .handle[F]
                      .foldF(
                        err => HttpUtils.rpcErrorToResponse(err),
                        r => Ok((r: ApiResponse).asJson.noSpaces)
                    )
                )
          }
        }
    }
  }
}
