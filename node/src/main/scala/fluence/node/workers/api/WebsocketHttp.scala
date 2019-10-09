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
import fluence.log.LogFactory
import fluence.node.workers.WorkersPorts
import fluence.worker.WorkersPool
import fluence.worker.responder.WorkerResponder
import fs2.concurrent.Queue
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import shapeless.{ops, HList}

import scala.language.higherKinds

object WebsocketHttp {

  /*def routes[F[_]: Sync: LogFactory, RS <: HList, CS <: HList](
    pool: WorkersPool[F, RS, CS]
  )(
    implicit dsl: Http4sDsl[F],
    p2p: ops.hlist.Selector[RS, WorkersPorts.P2pPort[F]],
    resp: ops.hlist.Selector[CS, WorkerResponder[F]]
  ): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case GET -> Root / LongVar(appId) / "ws" =>
        LogFactory[F].init("http" -> "websocket", "app" -> appId.toString) >>= { implicit log =>
          withApi(appId) { w ⇒
            w.websocket().flatMap { ws =>
              val processMessages: fs2.Pipe[F, WebSocketFrame, WebSocketFrame] =
                _.evalMap {
                  case Text(msg, _) =>
                    ws.processRequest(msg).map(Text(_))
                  case Close(data) =>
                    ws.closeWebsocket().map(_ => Text("Closing websocket"))
                  case m => log.error(s"Unsupported message: $m") as Text("Unsupported")
                }

              // TODO add maxSize to a config
              Queue
                .bounded[F, WebSocketFrame](32)
                .flatMap { q =>
                  val d = q.dequeue.through(processMessages).merge(ws.subscriptionEventStream.map(Text(_)))
                  val e = q.enqueue
                  WebSocketBuilder[F].build(d, e)
                }
            }
          }
        }
    }
  }*/
}
