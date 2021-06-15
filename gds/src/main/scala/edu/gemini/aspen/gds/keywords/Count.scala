package edu.gemini.aspen.gds.keywords

import cats.effect.{ Ref, Sync }
import cats.syntax.all._

trait Count[F[_]] {
  def incr: F[Unit]
  def decr: F[Unit]
}

object Count {
  def apply[F[_]: Sync]: F[Count[F]] =
    Ref[F].of(0).map { ref =>
      new Count[F] {
        def incr: F[Unit] = ref.update(_ + 1)
        def decr: F[Unit] = ref.update(_ - 1)
      }
    }
}
