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

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed abstract class WorkerStage(val hasWorker: Boolean = false)

object WorkerStage {

  case object NotInitialized extends WorkerStage()

  case object InitializationStarted extends WorkerStage()

  case object FullyAllocated extends WorkerStage(true)

  case object Stopping extends WorkerStage()

  // TODO can restart here
  case object Stopped extends WorkerStage()
  case object Destroying extends WorkerStage()
  case object Destroyed extends WorkerStage()

  implicit val encoder: Encoder[WorkerStage] = deriveEncoder
  implicit val decoder: Decoder[WorkerStage] = deriveDecoder
}
