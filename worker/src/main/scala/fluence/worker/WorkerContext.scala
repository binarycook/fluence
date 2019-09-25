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

package fluence.worker

import cats.data.EitherT
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.either._
import cats.effect.{Concurrent, Fiber, Resource}
import cats.effect.concurrent.{Deferred, Ref}
import fluence.effects.resources.MakeResource
import fluence.log.Log

import scala.language.higherKinds

/**
 * WorkerContext describes all the resources used by Worker and their lifecycles.
 *
 * Control ---- Join! -------------------------- Stop! --------------------------------------- Destroy! ----------------------------
 * Context ---- Init ---------------------------------------------------------(may be closed)----------------- Not usable anymore --
 * Resources ------ Prepare --------------------------------------------------(kept in place)-----X-- Destroy ----------------------
 * Worker ------------------- Allocate --------------------------- Deallocate ------------------------------------------------------
 * Companions ------------------------- Allocate --X-- Deallocate ------------------------------------------------------------------
 */
trait WorkerContext[F[_], R, C] {
  def stage: F[WorkerStage]

  def stages: fs2.Stream[F, WorkerStage]

  def app: eth.EthApp

  def resources: R

  def worker: EitherT[F, WorkerStage, Worker[F]]

  def companions: EitherT[F, WorkerStage, C]

  /**
   * Trigger Worker stop, releasing all acquired resources.
   * Stopping is performed asynchronously, you can track it with [[stage]]
   *
   */
  def stop()(implicit log: Log[F]): F[Unit]

  /**
   * Stop the worker and then destroy all the prepared [[resources]].
   * Worker must be re-initialized from scratch after that. Operation cannot be reverted.
   * Destruction is performed asynchronously, you can track it with [[stage]]
   */
  def destroy()(implicit log: Log[F]): F[Fiber[F, Unit]]
}

object WorkerContext {

  def apply[F[_]: Concurrent, R, W0 <: Worker[F], C](
    _app: eth.EthApp,
    workerResource: WorkerResource[F, R],
    worker: R ⇒ Resource[F, W0],
    companions: WorkerCompanion.Aux[F, C, W0]
  )(implicit log: Log[F]): F[WorkerContext[F, R, C]] =
    workerResource.prepare() >>= { res ⇒
      for {
        // Provide WorkerStage info within Ref for regular access, and with Queue to enable subscriptions
        stageRef ← Ref.of[F, WorkerStage](WorkerStage.NotInitialized)
        stageQueue ← fs2.concurrent.Queue.circularBuffer[F, WorkerStage](1)
        _ ← stageQueue.enqueue1(WorkerStage.NotInitialized)

        // Push stage updates to both queue and ref
        setStage = (s: WorkerStage) ⇒ stageQueue.enqueue1(s) *> stageRef.set(s)

        // We will get Worker, Companions and Stop callback later
        // TODO: if we want context to be restartable, these needs to be Queues?
        workerDef ← Deferred[F, Worker[F]]
        companionsDef ← Deferred[F, C]
        stopDef ← Deferred[F, F[Unit]]

        // Allocate Worker and Resources with no blocking
        _ ← MakeResource.useConcurrently[F](
          stop ⇒
            // Wrap the whole Worker/Companions lifecycle with stage-reflecting resource
            Resource.make[F, Unit](
              setStage(WorkerStage.InitializationStarted)
            )(_ ⇒ setStage(WorkerStage.Stopped)) >>
              // Worker allocate function may take a lot of time: it waits for Resources, runs Docker, etc.
              worker(res)
                .flatTap(
                  w ⇒
                    // We have worker, but no companions yet
                    Resource.liftF(
                      workerDef.complete(w) *>
                        setStage(WorkerStage.RunningCompanions)
                    )
                ) >>= (
              w ⇒
                // Everything is ready
                companions.resource(w) >>= (
                  wx ⇒
                    Resource.liftF(
                      stopDef.complete(stop) *>
                        companionsDef.complete(wx) *>
                        setStage(WorkerStage.FullyAllocated)
                    )
                  )
              )
        )
      } yield new WorkerContext[F, R, C] {
        override def app: eth.EthApp = _app

        override def stage: F[WorkerStage] = stageRef.get

        override def stages: fs2.Stream[F, WorkerStage] = stageQueue.dequeue

        override val resources: R = res

        override def worker: EitherT[F, WorkerStage, Worker[F]] =
          EitherT(stage.flatMap {
            case s if s.hasWorker ⇒ workerDef.get.map(_.asRight)
            case s ⇒ s.asLeft[Worker[F]].pure[F]
          })

        override def companions: EitherT[F, WorkerStage, C] =
          EitherT(stage.flatMap {
            case s if s.hasCompanions ⇒ companionsDef.get.map(_.asRight)
            case s ⇒ s.asLeft[C].pure[F]
          })

        override def stop()(implicit log: Log[F]): F[Unit] =
          stage.flatMap {
            case s if s.hasCompanions || s.hasWorker ⇒
              setStage(WorkerStage.Stopping) >>
                stopDef.get.flatten
            case _ ⇒
              ().pure[F]
          }

        override def destroy()(implicit log: Log[F]): F[Fiber[F, Unit]] =
          stop() >>
            Concurrent[F].start(
              setStage(WorkerStage.Destroying) >>
                workerResource.destroy().value.void >>
                setStage(WorkerStage.Destroyed)
            )
      }

    }
}
