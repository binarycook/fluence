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

package fluence.effects.tendermint.block.history

import java.nio.charset.Charset

import fluence.effects.tendermint.block.data.Header
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import proto3.tendermint.Vote
import scodec.bits.ByteVector
import fluence.effects.tendermint.block.history.helpers.ByteVectorJsonCodec
import fluence.effects.tendermint.block.data.ReencodingJsonCodecs
import io.circe.Decoder.Result

/**
 * Manifest of the block, as described in Fluence paper
 *
 * @param vmHash Hash of the vm state after the previous block
 * @param previousManifestReceipt Storage receipt for manifest on the previous block
 * @param txsReceipt Storage receipt on txs for the current block
 * @param header Block header
 * @param votes Votes (commits) on the previous blockID
 * @param emptyBlocksReceipts TODO
 */
case class BlockManifest(
  vmHash: ByteVector,
  previousManifestReceipt: Option[Receipt],
  txsReceipt: Option[Receipt],
  header: Header,
  votes: List[Vote],
  emptyBlocksReceipts: List[Receipt]
) {

  // TODO: Avoid using JSON since it's not a stable serialization. Maybe use protobuf? Or something custom.
  def jsonBytes(): ByteVector =
    ByteVector(jsonString.getBytes())

  lazy val jsonString: String = {
    import io.circe.syntax._
    (this: BlockManifest).asJson.noSpaces
  }
}

object BlockManifest {
  import fluence.effects.tendermint.block.data.SimpleJsonCodecs.Encoders.{
    byteVectorEncoder,
    headerEncoder,
    messageEncoder
  }
  import fluence.effects.tendermint.block.data.SimpleJsonCodecs.Decoders.{byteVectorDecoder, headerDecoder, voteDecoder}

  implicit val dec: Decoder[BlockManifest] = deriveDecoder[BlockManifest]
  implicit val enc: Encoder[BlockManifest] = deriveEncoder[BlockManifest]

  def fromBytes(bytes: ByteVector): Either[Exception, BlockManifest] = {
    import io.circe.parser._

    try {
      bytes
        .decodeString(Charset.defaultCharset())
        .flatMap(parse)
        .flatMap(_.as[BlockManifest])
    } catch {
      case e: Exception => Left(e)
    }
  }
}
