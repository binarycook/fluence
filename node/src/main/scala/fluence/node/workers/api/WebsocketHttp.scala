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
