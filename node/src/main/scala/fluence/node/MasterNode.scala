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

package fluence.node
import java.nio.file._

import cats.Applicative
import cats.effect._
import cats.effect.syntax.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fluence.effects.{Backoff, EffectError}
import fluence.effects.castore.StoreError
import fluence.effects.ethclient.EthClient
import fluence.effects.ipfs.IpfsStore
import fluence.effects.sttp.SttpStreamEffect
import fluence.effects.swarm.SwarmStore
import fluence.kad.Kademlia
import fluence.log.{Log, LogFactory}
import fluence.node.code.{CodeCarrier, LocalCodeCarrier, PolyStore, RemoteCodeCarrier}
import fluence.node.config.storage.RemoteStorageConfig
import fluence.node.config.{MasterConfig, NodeConfig}
import fluence.node.eth._
import fluence.node.workers.tendermint.config.ConfigTemplate
import fluence.statemachine.api.command.PeersControl
import fluence.worker.eth.{EthApp, StorageType}

import scala.language.{higherKinds, postfixOps}

/**
 * Represents a MasterNode process. Takes cluster forming events from Ethereum, and spawns new Workers to serve them.
 *
 * @param nodeEth Ethereum adapter
 * @param pool Workers pool to launch workers in
 */
case class MasterNode[F[_]: ConcurrentEffect: LiftIO: LogFactory, C](
  nodeEth: NodeEth[F],
  pool: MasterPool.Type[F]
)(implicit backoff: Backoff[EffectError]) {

  /**
   * Runs app worker on a pool
   *
   * @param app App description
   */
  def runAppWorker(app: EthApp): F[Unit] =
    for {
      implicit0(log: Log[F]) ← LogFactory[F].init("app", app.id.toString)
      _ ← log.info("Running worker")
      _ <- pool.run(app)
    } yield ()

  /**
   * Runs the appropriate effect for each incoming NodeEthEvent, keeping it untouched
   */
  def handleEthEvent(implicit log: Log[F]): fs2.Pipe[F, NodeEthEvent, NodeEthEvent] =
    _.evalTap {
      case RunAppWorker(app) ⇒
        runAppWorker(app)

      case RemoveAppWorker(appId) ⇒
        pool.get(appId).semiflatMap(_.destroy()).value.void

      case DropPeerWorker(appId, vk) ⇒
        Log[F].scope("app" -> appId.toString, "key" -> vk.toHex) { implicit log: Log[F] =>
          pool
            .getCompanion[PeersControl[F]](appId)
            .semiflatMap(
              _.dropPeer(vk).valueOr(e ⇒ log.error(s"Unexpected error while dropping peer", e))
            )
            .valueOr(st ⇒ log.error(s"No available worker for $appId: it's on stage $st"))
        }

      case NewBlockReceived(_) ⇒
        Applicative[F].unit

      case ContractAppsLoaded ⇒
        Applicative[F].unit
    }

  /**
   * Runs master node and starts listening for AppDeleted event in different threads,
   * then joins the threads and returns back exit code from master node
   */
  def run(implicit log: Log[F]): IO[ExitCode] =
    nodeEth.nodeEvents
      .evalTap(ev ⇒ log.debug("Got NodeEth event: " + ev))
      .through(handleEthEvent)
      .compile
      .drain
      .toIO
      .attempt
      .flatMap {
        case Left(err) ⇒
          log.error("Execution failed", err) as ExitCode.Error toIO

        case Right(_) ⇒
          log.info("Execution finished") as ExitCode.Success toIO
      }
}

object MasterNode {

  // TODO drop unused args
  /**
   * Makes the MasterNode resource for the given config
   *
   * @param masterConfig MasterConfig
   * @param nodeConfig NodeConfig
   * @return Prepared [[MasterNode]], then see [[MasterNode.run]]
   */
  def make[F[_]: ConcurrentEffect: LiftIO: ContextShift: Timer: Log: LogFactory: SttpStreamEffect, C](
    masterConfig: MasterConfig,
    nodeConfig: NodeConfig,
    pool: MasterPool.Type[F],
    kademlia: Kademlia[F, C]
  )(
    implicit
    backoff: Backoff[EffectError]
  ): Resource[F, MasterNode[F, C]] =
    for {
      ethClient ← EthClient.make[F](Some(masterConfig.ethereum.uri))

      _ ← Log.resource[F].debug("-> going to create nodeEth")

      nodeEth ← NodeEth[F](nodeConfig.validatorKey.toByteVector, ethClient, masterConfig.contract)

      codeCarrier ← Resource.pure(codeCarrier[F](masterConfig.remoteStorage))

      rootPath <- Resource.liftF(IO(Paths.get(masterConfig.rootPath).toAbsolutePath).to[F])

      configTemplate ← Resource.liftF(ConfigTemplate[F](rootPath, masterConfig.tendermintConfig))

    } yield MasterNode[F, C](
      nodeEth,
      pool
    )

  def codeCarrier[F[_]: Sync: ContextShift: Concurrent: Timer: LiftIO: SttpStreamEffect](
    config: RemoteStorageConfig
  ): CodeCarrier[F] =
    if (config.enabled) {
      implicit val b: Backoff[StoreError] = Backoff.default
      val swarmStore = SwarmStore[F](config.swarm.address, config.swarm.readTimeout)
      val ipfsStore = IpfsStore[F](config.ipfs.address, config.ipfs.readTimeout)
      val polyStore = new PolyStore[F]({
        case StorageType.Swarm => swarmStore
        case StorageType.Ipfs  => ipfsStore
      })
      new RemoteCodeCarrier[F](polyStore)
    } else
      new LocalCodeCarrier[F]()
}
