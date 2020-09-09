package datasketches.sandbox

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import datasketches.sandbox.sketch.DistinctCount

case class Environment[F[_]](
    parallelism: Int,
    exactCount: DistinctCount[F],
    sketchCount: DistinctCount[F]
)

object Environment {
  private lazy val defaultAccuracy = 16
  private lazy val defaultParallelism = 2

  def apply[F[_]: Sync]: F[Environment[F]] =
    for {
      accuracy <- Sync[F].pure(
        Option(System.getenv("SKETCH_ACCURACY"))
          .map(_.toInt)
          .getOrElse(defaultAccuracy)
      )
      parallelism <- Sync[F].pure(
        Option(System.getenv("PARALLELISM"))
          .map(_.toInt)
          .getOrElse(defaultParallelism)
      )
      exactCount <- DistinctCount.exact[F]
      sketchCount <- DistinctCount.sketch[F](accuracy)
    } yield new Environment[F](parallelism, exactCount, sketchCount)
}
