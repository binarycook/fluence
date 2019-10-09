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
import cats.data.EitherT
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.effect.{Concurrent, Sync, Timer}
import fluence.log.{Log, LogFactory}
import fluence.node.workers.WorkersPorts
import fluence.worker.{Worker, WorkersPool}
import fluence.worker.responder.WorkerResponder
import io.circe.syntax._
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import shapeless.{ops, HList}
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.higherKinds

object WorkerHttp {

  def readRequest[F[_]: Sync: Http4sDsl]: PartialFunction[Request[F], F[ApiRequest]] =
    BasicHttp.readRequest[F] orElse SendAndWaitHttp.readRequest[F]

  // ROUTES
  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    p2p: ops.hlist.Selector[RS, WorkersPorts.P2pPort[F]],
    resp: ops.hlist.Selector[CS, WorkerResponder[F]]
  ): HttpRoutes[F] = {
    import dsl._

    object QueryWait extends OptionalQueryParamDecoderMatcher[Int]("wait")

    def status(appId: Long, timeout: FiniteDuration)(implicit log: Log[F]): F[Response[F]] =
      pool
        .getWorker(appId)
        .foldF(
          stage => HttpUtils.stageToResponse(appId, stage),
          w => w.status(timeout).flatMap(s ⇒ Ok(s.asJson.spaces2))
        )

    def handle(worker: Worker[F, CS])(implicit log: Log[F]): ApiRequest ⇒ EitherT[F, ApiErrorT, ApiResponse] =
      (
        BasicHttp.handleRequest(worker) orElse SendAndWaitHttp.handleRequest(worker)
      ).lift(_).getOrElse(EitherT.leftT(ApiError("Handler not found")))

    HttpRoutes.of[F] {
      case GET -> Root / LongVar(appId) / "p2pPort" ⇒
        LogFactory[F].init("http" -> "p2pPort", "app" -> appId.toString) >>= { implicit log =>
          log.trace(s"Worker p2pPort") *>
            pool
              .getResources(appId)
              .map(_.select[WorkersPorts.P2pPort[F]])
              .semiflatMap(_.port)
              .value
              .flatMap {
                case Some(port) ⇒ Ok(port.toString)
                case None ⇒ NotFound()
              }
        }

      case GET -> Root / LongVar(appId) / "status" :? QueryWait(wait) ⇒
        LogFactory[F].init("http" -> "status", "app" -> appId.toString) >>= { implicit log =>
          // Fetches the worker's status, waiting no more than 10 seconds (if ?wait=$SECONDS is provided), or 1 second otherwise
          status(appId, wait.filter(_ < 10).fold(1.second)(_.seconds))
        }

      case req @ (GET | POST) -> Root / LongVar(appId) ⇒
        // TODO move request caret there
        LogFactory[F].init("app" -> appId.toString) >>= { implicit log ⇒
          readRequest[F]
            .lift(req)
            .fold(
              BadRequest("Cannot parse request")
            )(
              apiReq ⇒
                pool
                  .getWorker(appId)
                  .foldF(
                    HttpUtils.stageToResponse(appId, _),
                    w ⇒
                      (EitherT.right[ApiErrorT](apiReq) >>= handle(w)).foldF(
                        HttpUtils.rpcErrorToResponse(_),
                        resp ⇒ Ok(resp.asJson.noSpaces)
                      )
                  )
            )
        }
    }
  }
}
