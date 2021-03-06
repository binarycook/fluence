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

package fluence.worker.responder

import cats.effect.{ContextShift, IO, Timer}
import fluence.bp.tx.Tx
import fluence.effects.{Backoff, EffectError}
import fluence.log.{Log, LogFactory}
import fluence.log.LogFactory.Aux
import fluence.log.appender.PrintlnLogAppender
import fluence.worker.responder.repeat.{SubscriptionKey, SubscriptionStorage}
import org.scalatest.{Matchers, OptionValues, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SubscriptionStorageSpec extends WordSpec with OptionValues with Matchers {
  implicit private val ioTimer: Timer[IO] = IO.timer(global)
  implicit private val ioShift: ContextShift[IO] = IO.contextShift(global)
  implicit private val backoff: Backoff[EffectError] = Backoff.default[EffectError]
  implicit private val logFactory: Aux[IO, PrintlnLogAppender[IO]] = LogFactory.forPrintln[IO](level = Log.Error)
  implicit private val log: Log.Aux[IO, PrintlnLogAppender[IO]] =
    logFactory.init("SubscriptionStorageSpec", level = Log.Off).unsafeRunTimed(5.seconds).value

  private def createStorageAndSubscription(): IO[(SubscriptionStorage[IO], SubscriptionKey, Tx.Data)] = {
    for {
      storage <- SubscriptionStorage[IO]()
      tx = "123"
      txData = Tx.Data(tx.getBytes())
      key = SubscriptionKey.generate("subscriptionId", txData)
    } yield (storage, key, txData)

  }

  "subscription storage" should {
    "return false on adding subscription twice" in {
      (for {
        (storage, key, txData) <- createStorageAndSubscription()
        add1Result <- storage.addSubscription(key, txData)
        _ = add1Result shouldBe true
        add2Result <- storage.addSubscription(key, txData)
        _ = add2Result shouldBe false
      } yield ()).unsafeRunSync()
    }

    "return stream after adding subscription and stream" in {
      (for {
        (storage, key, txData) <- createStorageAndSubscription()
        add1Result <- storage.addSubscription(key, txData)
        _ = add1Result shouldBe true
        subs <- storage.getSubscriptions
      } yield subs.get(key) shouldBe defined).unsafeRunSync()
    }

    "delete subscription on delete operation" in {
      (for {
        (storage, key, txData) <- createStorageAndSubscription()
        add1Result <- storage.addSubscription(key, txData)
        _ = add1Result shouldBe true
        _ <- storage.deleteSubscription(key)
        subs <- storage.getSubscriptions
      } yield subs.get(key) shouldBe empty).unsafeRunSync()

    }
  }
}
