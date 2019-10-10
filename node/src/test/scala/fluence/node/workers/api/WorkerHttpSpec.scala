package fluence.node.workers.api

import cats.data.EitherT
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, Fiber, IO, Timer}
import fluence.log.{Log, LogFactory}
import fluence.worker.eth.EthApp
import fluence.worker.responder.WorkerResponder
import fluence.worker.{Worker, WorkerContext, WorkerStage, WorkersPool}
import org.scalatest.{Matchers, WordSpec}
import shapeless.{::, HNil}
import org.http4s._
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

  def context(): WorkerContext[IO, Resources, Companions] = {
    val stage = IO(WorkerStage.FullyAllocated)
    val stages = fs2.Stream.empty
    val worker: EitherT[IO, WorkerStage, Worker[IO, Companions]] = EitherT.fromEither[IO](Left(stage.unsafeRunSync()))

    new WorkerContext[IO, Resources, Companions](stage, stages, 10L, HNil, worker) {
      override def stop()(implicit log: Log[IO]): IO[Unit] = ???
      override def destroy()(implicit log: Log[IO]): IO[Fiber[IO, Unit]] = ???
    }
  }

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

    "c" in {

      (for {
        ref <- Ref.of[IO, Map[Long, WorkerContext[IO, Resources, Companions]]](Map.empty)
        pool = new WorkersPool[IO, Resources, Companions](ref, (_, _) => IO(context()))
        _ <- pool.run(EthApp(10, null, null))
        routes = WorkerHttp.routes[IO, Resources, Companions](pool)

        resp <- routes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/10/query?path=123")
        )
        r <- resp.as[String]
      } yield {
        println(resp)
        println(r)
        resp.status.code shouldBe 404
      }).unsafeRunSync()
    }
  }
}
