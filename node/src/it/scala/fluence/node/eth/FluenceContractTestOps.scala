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

package fluence.node.eth

import cats.Monad
import cats.effect.{LiftIO, Timer}
import cats.syntax.functor._
import fluence.effects.ethclient.helpers.Web3jConverters
import fluence.effects.ethclient.helpers.Web3jConverters.{nodeAddressToBytes24, stringToBytes32}
import fluence.effects.ethclient.syntax._
import fluence.node.workers.tendermint.ValidatorPublicKey
import fluence.worker.eth.StorageType
import fluence.worker.eth.StorageType.StorageType
import org.web3j.abi.datatypes.generated._
import org.web3j.abi.datatypes.{Bool, DynamicArray}

import scala.language.higherKinds

object FluenceContractTestOps {
  implicit class RichFluenceContract(fc: FluenceContract) {
    import fc.contract

    /**
     * Register the node in the contract.
     * TODO check permissions, Ethereum public key should match
     *
     * @param nodeAddressHex p2p ID for this node. Basically first 20 bytes of p2p peer SHA256(PubKey)
     * @return The block number where transaction has been mined
     */
    def addNode[F[_]: LiftIO: Timer: Monad](
      validatorKey: String,
      nodeAddressHex: String,
      nodeIP: String,
      apiPort: Short,
      capacity: Short
    ): F[BigInt] =
      contract
        .addNode(
          Web3jConverters.base64ToBytes32(validatorKey),
          nodeAddressToBytes24(nodeIP, nodeAddressHex),
          new Uint16(apiPort),
          new Uint16(capacity),
          new Bool(false)
        )
        .callUntilSuccess[F]
        .map(_.getBlockNumber)
        .map(BigInt(_))

    /**
     * Publishes a new app to the Fluence Network
     *
     * @param storageHash Hash of the code in Swarm
     * @param clusterSize Cluster size required to host this app
     * @tparam F Effect
     * @return The block number where transaction has been mined
     */
    def addApp[F[_]: LiftIO: Timer: Monad](
      storageHash: String,
      storageType: StorageType = StorageType.Swarm,
      clusterSize: Short = 1
    ): F[BigInt] =
      contract
        .addApp(
          stringToBytes32(storageHash),
          stringToBytes32("receipt_stub"),
          new Bytes32(Array.concat(Array.fill[Byte](31)(0), Array(StorageType.toByte(storageType)))),
          new Uint8(clusterSize),
          new DynamicArray[Bytes32](classOf[Bytes32])
        )
        .callUntilSuccess[F]
        .map(_.getBlockNumber)
        .map(BigInt(_))

    /**
     * Deletes deployed app from contract, triggering AppDeleted event on successful deletion
     *
     * @param appId 32-byte id of the app to be deleted
     * @tparam F Effect
     */
    def deleteApp[F[_]: LiftIO: Timer: Monad](appId: Long): F[Unit] =
      contract.deleteApp(new Uint256(appId)).callUntilSuccess[F].void
  }
}
