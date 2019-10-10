package fluence.node.workers.api

import cats.Parallel
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.flatMap._
import fluence.log.LogFactory
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request}
import shapeless.HList

import scala.language.higherKinds

object WorkerHttp {

  def readRequest[F[_]: Sync: Http4sDsl]: PartialFunction[Request[F], F[ApiRequest]] =
    BasicHttp.readRequest[F] orElse SendAndWaitHttp.readRequest[F]

  // ROUTES
  def routes[F[_]: Sync: LogFactory: Concurrent: Timer: Parallel](
    requestHandler: RequestHandler[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case req @ (GET | POST) -> Root / LongVar(appId) / _ ⇒
        // TODO move request caret there
        LogFactory[F].init("app" -> appId.toString) >>= { implicit log ⇒
          readRequest[F]
            .lift(req)
            .fold(
              BadRequest("Cannot parse request")
            )(
              apiReq ⇒
                apiReq.flatMap(
                  r =>
                    requestHandler
                      .handle(appId, r)
                      .foldF(
                        HttpUtils.rpcErrorToResponse(_),
                        resp ⇒ Ok(resp.asJson.noSpaces)
                    )
              )
            )
        }
    }
  }
}
