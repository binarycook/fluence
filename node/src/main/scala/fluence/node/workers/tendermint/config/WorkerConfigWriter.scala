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

package fluence.node.workers.tendermint.config
import java.nio.file.{Files, Path, StandardCopyOption}

import cats.Functor
import cats.effect.{IO, LiftIO}
import fluence.node.config.Configuration
import fluence.node.eth.state.App

import scala.io.Source
import scala.language.higherKinds

object WorkerConfigWriter extends slogging.LazyLogging {

  case class WorkerConfigPaths(templateConfigDir: Path, workerPath: Path, workerConfigDir: Path)

  /**
   * Resolves source (template) and target (for specific worker) configuration paths
   * Creates target configuration directory if it doesn't exists
   * @param rootPath Path to resolve against, usually /master inside Master container
   * @return original App and config paths wrapped in WorkerConfigPaths
   */
  def resolveWorkerConfigPaths[F[_]: LiftIO](rootPath: Path): fs2.Pipe[F, App, (App, WorkerConfigPaths)] =
    _.evalMap { app =>
      (for {
        tmDir ← IO(rootPath.resolve("tendermint"))
        templateConfigDir ← IO(tmDir.resolve("config"))
        workerPath ← IO(tmDir.resolve(s"${app.id}_${app.cluster.currentWorker.index}"))
        workerConfigDir ← IO(workerPath.resolve("config"))
        _ ← IO { Files.createDirectories(workerConfigDir) }
      } yield (app, WorkerConfigPaths(templateConfigDir, workerPath, workerConfigDir))).to[F]
    }

  /**
   * Generate, copy and/or update different configs used by tendermint.
   *
   * `rootPath` is usually /master inside Master container
   * `templateConfigDir` contain:
   *    - configs generated by `tendermint --init` (see [[Configuration.tendermintInit]])
   *    - config/default_config.toml, copied on container build (see node's dockerfile in build.sbt)
   *
   * At the end of execution `workerPath` will contain:
   *    - tendermint configuration in `workerConfigDir`:
   *        - node_key.json, containing private P2P key
   *        - priv_validator.json, containing validator's private & public keys and it's address
   *        - genesis.json, generated from [[App.cluster]] and [[App.id]]
   *        - config.toml, copied from `templateConfigDir/default_config.toml` and updated
   */
  def writeConfigs[F[_]: LiftIO: Functor]: fs2.Pipe[F, (App, WorkerConfigPaths, Path), (App, WorkerConfigPaths, Path)] =
    _.evalTap {
      case (app, paths, _) =>
        (
          for {
            _ ← WorkerConfigWriter.copyMasterKeys(paths.templateConfigDir, paths.workerConfigDir)
            _ ← WorkerConfigWriter.writeGenesis(app, paths.workerConfigDir)
            _ ← WorkerConfigWriter.updateConfigTOML(
              app,
              configSrc = paths.templateConfigDir.resolve("default_config.toml"),
              configDest = paths.workerConfigDir.resolve("config.toml")
            )

          } yield ()
        ).to[F]
    }

  private def writeGenesis(app: App, dest: Path): IO[Unit] = IO {
    val genesis = GenesisConfig.generateJson(app)

    logger.info("Writing {}/genesis.json", dest)
    Files.write(dest.resolve("genesis.json"), genesis.getBytes)
  }

  private def updateConfigTOML(app: App, configSrc: Path, configDest: Path): IO[Unit] = IO {
    import scala.collection.JavaConverters._
    logger.info("Updating {} -> {}", configSrc, configDest)

    val currentWorker = app.cluster.currentWorker
    val persistentPeers = app.cluster.workers.map(_.peerAddress).mkString(",")

    val lines = Source.fromFile(configSrc.toUri).getLines().map {
      case s if s.contains("external_address") => s"""external_address = "${currentWorker.address}""""
      case s if s.contains("persistent_peers") => s"""persistent_peers = "$persistentPeers""""
      case s if s.contains("moniker") =>
        s"""moniker = "${app.id}_${currentWorker.index}""""
      case s => s
    }

    Files.write(configDest, lines.toIterable.asJava)
  }

  private def copyMasterKeys(from: Path, to: Path): IO[Unit] = {
    import StandardCopyOption.REPLACE_EXISTING

    val nodeKey = "node_key.json"
    val validator = "priv_validator.json"

    IO {
      logger.info(s"Copying keys to worker: ${from.resolve(nodeKey)} -> ${to.resolve(nodeKey)}")
      Files.copy(from.resolve(nodeKey), to.resolve(nodeKey), REPLACE_EXISTING)

      logger.info(s"Copying priv_validator to worker: ${from.resolve(validator)} -> ${to.resolve(validator)}")
      Files.copy(from.resolve(validator), to.resolve(validator), REPLACE_EXISTING)
    }
  }
}
