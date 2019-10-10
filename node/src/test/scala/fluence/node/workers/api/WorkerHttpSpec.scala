package fluence.node.workers.api

import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Timer}
import fluence.log.{Log, LogFactory}
import fluence.node.workers.{WorkerFiles, WorkersPorts}
import fluence.statemachine.api.command.PeersControl
import fluence.worker.eth.EthApp
import fluence.worker.responder.WorkerResponder
import fluence.worker.{WorkerContext, WorkersPool}
import org.scalatest.{Matchers, WordSpec}
import shapeless.{::, HNil}
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

class WorkerHttpSpec extends WordSpec with Matchers {
  implicit val ioShift: ContextShift[IO] = IO.contextShift(global)

  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val logFactory = LogFactory.forPrintln[IO]()
  implicit val log: Log[IO] = logFactory.init("WorkersPortsSpec").unsafeRunSync()

  implicit val http4sDsl = org.http4s.dsl.io

  type Resources = HNil
  type Companions = WorkerResponder[IO] :: HNil

  "" should {
    "a" in {

      (for {
        ref <- Ref.of[IO, Map[Long, WorkerContext[IO, Resources, Companions]]](Map.empty)
        pool = new WorkersPool[IO, Resources, Companions](ref, (_, _) => IO.never)
        routes = WorkerHttp.routes[IO, Resources, Companions](pool)
        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/1/some")
        )
        r <- resp.as[String]
      } yield { resp.status.code shouldBe 400 }).unsafeRunSync()
    }

    "b" in {

      (for {
        ref <- Ref.of[IO, Map[Long, WorkerContext[IO, Resources, Companions]]](Map.empty)
        pool = new WorkersPool[IO, Resources, Companions](ref, (_, _) => IO.never)
        routes = WorkerHttp.routes[IO, Resources, Companions](pool)
        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/1/query?path=123")
        )
        r <- resp.as[String]
      } yield { resp.status.code shouldBe 404 }).unsafeRunSync()
    }
  }
}
