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

package fluence

import cats.arrow.Category
import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._

package object morph {
  type =?>[A, B] = Kleisli[Either[MorphError, *], A, B]

  type Into[A, B] = A =?> B

  type From[B, A] = A =?> B

  type <=?>[A, B] = (A =?> B, B =?> A)

  type FromInto[A, B] = A <=?> B

  implicit def identityMorph[T]: T =?> T = Kleisli(t ⇒ t.asRight)

  implicit def swapBiMorph[A, B](implicit ab: A <=?> B): B <=?> A = ab.swap

  implicit def pickMorphDirect[A, B](implicit ab: A <=?> B): A =?> B = ab._1

  implicit def pickMorphInverse[A, B](implicit ab: A <=?> B): A From B = ab._2

  implicit object BiMorphCategory extends Category[<=?>] {
    override def id[A]: (A =?> A, A =?> A) =
      (identityMorph, identityMorph)

    override def compose[A, B, C](f: (B =?> C, C =?> B), g: (A =?> B, B =?> A)): (A =?> C, C =?> A) =
      (f._1 compose g._1, f._2 andThen g._2)
  }
}
