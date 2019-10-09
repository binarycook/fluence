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

package fluence.statemachine.abci.block

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.instances.either._
import cats.syntax.functor._
import fluence.bp.tx.Tx
import fluence.effects.EffectError
import fluence.effects.tendermint.block.TendermintBlock
import fluence.effects.tendermint.block.data.{Base64ByteVector, Block, Data, Header, LastCommit}
import proto3.tendermint.{BlockID, PartSetHeader, Version, Vote}
import com.github.jtendermint.jabci.types.{Vote => ABCIVote}
import com.github.jtendermint.jabci.types.{BlockID => ABCIBlockID}
import com.github.jtendermint.jabci.types.{PartSetHeader => ABCIPartSetHeader}
import com.github.jtendermint.jabci.types.{Version => ABCIVersion}
import com.github.jtendermint.jabci.types.{Header => ABCIHeader}
import com.google.protobuf.{ByteString, Timestamp => ABCITimestamp}
import com.google.protobuf.timestamp.Timestamp
import scodec.bits.ByteVector

import scala.language.higherKinds

case class PartialBlock(
  // from BeginBlock at N
  header: Option[Header] = None,
  // from Commit at N
  txs: List[Tx] = Nil,
  // from BeginBlock at N+1
  votes: Option[List[Vote]] = None,
  // from BeginBlock at N+1
  blockId: Option[Option[BlockID]] = None
)

case class BlockNotReadyError(field: String) extends EffectError {
  override def getMessage: String = s"$field is empty"
}

object ABCIConverter {

  def timestamp(ts: ABCITimestamp): Timestamp = Timestamp(ts.getSeconds, ts.getNanos)
  def partsHeader(header: ABCIPartSetHeader): PartSetHeader = PartSetHeader(header.getTotal, header.getHash)

  def vote(vote: ABCIVote): Vote =
    Vote(
      vote.getType,
      vote.getHeight,
      vote.getRound,
      Option(vote.getBlockId).map(blockId),
      timestamp = Option(vote.getTimestamp).map(timestamp),
      vote.getValidatorAddress,
      vote.getValidatorIndex,
      vote.getSignature
    )

  def blockId(blockId: ABCIBlockID): BlockID = BlockID(blockId.getHash, Option(blockId.getPartsHeader).map(partsHeader))
  def version(version: ABCIVersion): Version = Version(version.getBlock, version.getApp)

  private def bv(bs: ByteString) = ByteVector(bs.asReadOnlyByteBuffer)

  def header(header: ABCIHeader): Header = Header(
    Option(header.getVersion).map(version),
    header.getChainId,
    header.getHeight,
    Option(header.getTime).map(timestamp),
    header.getNumTxs,
    header.getTotalTxs,
    Option(header.getLastBlockId).map(blockId),
    bv(header.getLastCommitHash),
    bv(header.getDataHash),
    bv(header.getValidatorsHash),
    bv(header.getNextValidatorsHash),
    bv(header.getConsensusHash),
    bv(header.getAppHash),
    bv(header.getLastResultsHash),
    bv(header.getEvidenceHash),
    bv(header.getProposerAddress)
  )
}

class BlockCollector[F[_]: Monad](ref: Ref[F, PartialBlock]) {

  def addHeader(header: ABCIHeader) =
    ref.update(_.copy(header = Some(ABCIConverter.header(header))))

  def addTxs(txs: List[Tx]) =
    ref.update(_.copy(txs = txs))

  def addVotes(votes: List[ABCIVote]) =
    ref.update(_.copy(votes = Some(votes.map(ABCIConverter.vote))))

  def addBlockId(blockId: Option[ABCIBlockID]) =
    ref.update(_.copy(blockId = Some(blockId.map(ABCIConverter.blockId))))

  def getBlock(): F[Either[BlockNotReadyError, TendermintBlock]] =
    ref
      .modify(b => (PartialBlock(), b))
      .map(
        block =>
          for {
            header <- get(block.header, "header")
            votes <- get(block.votes, "votes")
            blockId <- get(block.blockId, "blockId")
            data = Data(block.txs.map(tx => Base64ByteVector(ByteVector(tx.generateTx()))).some)
            lastCommit = LastCommit(blockId, votes.map(Some(_)))
          } yield TendermintBlock(Block(header, data, lastCommit))
      )

  private def get[T](opt: Option[T], name: String) =
    opt.fold(BlockNotReadyError(name).asLeft[T])(_.asRight[BlockNotReadyError])
}

object BlockCollector {

  def apply[F[_]: Sync: Functor]: F[BlockCollector[F]] =
    Ref.of[F, PartialBlock](PartialBlock()).map(new BlockCollector[F](_))
}
