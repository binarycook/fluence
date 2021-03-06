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

package fluence.log.appender

import cats.{~>, Monad}
import fluence.log.Log
import scala.language.higherKinds

trait LogAppender[F[_]] {
  self ⇒
  private[log] def appendMsg(msg: Log.Msg): F[Unit]

  def mapK[G[_]: Monad](nat: F ~> G): LogAppender[G] =
    (msg: Log.Msg) ⇒ nat(self.appendMsg(msg))
}
