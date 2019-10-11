package fluence.node.workers.api

import cats.data.EitherT
import cats.effect.{ContextShift, IO, Timer}
import fluence.log.{Log, LogFactory}
import org.http4s._
import org.http4s.implicits._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class WorkerHttpSpec extends WordSpec with Matchers {
  implicit val ioShift: ContextShift[IO] = IO.contextShift(global)

  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val logFactory = LogFactory.forPrintln[IO]()
  implicit val log: Log[IO] = logFactory.init("WorkersPortsSpec").unsafeRunSync()

  implicit val http4sDsl = org.http4s.dsl.io

  "WorkerHttp" should {
    "return an error, if request is incorrect" in {
      val handler = new Handler[IO] {
        override def handle(appId: Long, apiRequest: ApiRequest)(
          implicit log: Log[IO]
        ): EitherT[IO, ApiErrorT, ApiResponse] = throw new NotImplementedError("def handle")
      }
      val routes = WorkerHttp.routes[IO](handler)
      (for {

        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/1/some")
        )
        r <- resp.as[String]
      } yield {
        println(r)
        resp.status.code shouldBe 400
      }).unsafeRunSync()
    }

    "return an error, if handler returns an error" in {
      val handler = new Handler[IO] {
        override def handle(appId: Long, apiRequest: ApiRequest)(
          implicit log: Log[IO]
        ): EitherT[IO, ApiErrorT, ApiResponse] =
          EitherT.fromEither(Left(UnexpectedApiError("unexpected"): ApiErrorT))
      }
      val routes = WorkerHttp.routes[IO](handler)
      (for {
        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/10/query?path=123")
        )
        r <- resp.as[String]
      } yield {
        println(r)
        resp.status.code shouldBe 500
      }).unsafeRunSync()
    }

    "return a response, if worker is responding ok" in {
      val handler = new Handler[IO] {
        override def handle(appId: Long, apiRequest: ApiRequest)(
          implicit log: Log[IO]
        ): EitherT[IO, ApiErrorT, ApiResponse] =
          EitherT.fromEither(Right(QueryResponse("result")))
      }
      val routes = WorkerHttp.routes[IO](handler)
      (for {
        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/10/query?path=123")
        )
        r <- resp.as[String]
      } yield {
        println(resp)
        println(r)
        resp.status.code shouldBe 200
      }).unsafeRunSync()
    }

  }
}
