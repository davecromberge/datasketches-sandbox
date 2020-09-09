package datasketches.sandbox.sketch

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import datasketches.sandbox.domain.{CardinalityEstimate, Identifier, Key}
import datasketches.sandbox.{MapRefState, State}
import fs2.Chunk
import org.apache.datasketches.Family
import org.apache.datasketches.theta._

trait DistinctCount[F[_]] {
  def set(ids: Chunk[Identifier], key: Key): F[Unit]
  def estimate(key: Key): F[CardinalityEstimate]
  def union(k1: Key, k2: Key): F[CardinalityEstimate]
  def intersect(k1: Key, k2: Key): F[CardinalityEstimate]
  def aNotb(k1: Key, k2: Key): F[CardinalityEstimate]
  def clear(key: Key): F[Unit]
}

object DistinctCount {
  final def sketch[F[_]: Sync](accuracy: Int): F[DistinctCount[F]] =
    MapRefState.apply[F, UpdateSketch].map(sketch(accuracy, _))
  final def sketch[F[_]: Sync](
      accuracy: Int,
      state: State[F, UpdateSketch]
  ): DistinctCount[F] =
    new DistinctCount[F] {
      private lazy val sigma: Int = 2

      override def set(ids: Chunk[Identifier], key: Key): F[Unit] =
        for {
          sk <- state.get(key, emptySketch)
          _ <- Sync[F].delay(ids.foreach { id =>
            sk.update(id.value); ()
          })
          _ <- state.set(key, sk)
        } yield ()

      override def estimate(key: Key): F[CardinalityEstimate] =
        state
          .get(key, emptySketch)
          .map(sk =>
            CardinalityEstimate(
              sk.getEstimate,
              sk.getLowerBound(sigma),
              sk.getUpperBound(sigma)
            )
          )

      override def clear(key: Key): F[Unit] = state.clear(key)

      override def union(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOperation(k1, k2) {
          case (sk1, sk2, builder) =>
            val op = builder.buildUnion()
            op.update(sk1)
            op.update(sk2)
            op.getResult
        }

      override def intersect(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOperation(k1, k2) {
          case (sk1, sk2, builder) =>
            val op = builder.buildIntersection()
            op.update(sk1)
            op.update(sk2)
            op.getResult
        }

      override def aNotb(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOperation(k1, k2) {
          case (sk1, sk2, builder) =>
            val op = builder.buildANotB()
            op.aNotB(sk1, sk2)
        }

      private def emptySketch =
        UpdateSketch
          .builder()
          .setLogNominalEntries(accuracy)
          .setFamily(Family.QUICKSELECT)
          .build()

      private def setOperation(k1: Key, k2: Key)(
          f: (Sketch, Sketch, SetOperationBuilder) => CompactSketch
      ): F[CardinalityEstimate] =
        for {
          sk1 <- state.get(k1, emptySketch)
          sk2 <- state.get(k2, emptySketch)
          sk3 <- Sync[F].delay {
            val builder = Sketches.setOperationBuilder()
            val nomEntries = math.pow(2.toDouble, accuracy.toDouble).toInt
            builder.setNominalEntries(nomEntries)
            f(sk1, sk2, builder)
          }
        } yield CardinalityEstimate(
          sk3.getEstimate,
          sk3.getLowerBound(sigma),
          sk3.getUpperBound(sigma)
        )
    }

  final def exact[F[_]: Sync]: F[DistinctCount[F]] =
    MapRefState.apply[F, Set[Identifier]].map(exact(_))
  final def exact[F[_]: Sync](
      state: State[F, Set[Identifier]]
  ): DistinctCount[F] =
    new DistinctCount[F] {
      override def set(ids: Chunk[Identifier], key: Key): F[Unit] =
        for {
          keySet <- state.get(key, Set.empty[Identifier])
          _ <- state.set(key, keySet ++ ids.toList.toSet)
        } yield ()

      override def estimate(key: Key): F[CardinalityEstimate] =
        state
          .get(key, Set.empty[Identifier])
          .map(exactCount)

      override def clear(key: Key): F[Unit] = state.clear(key)

      override def union(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOp(k1, k2)(_ ++ _).map(exactCount)

      override def intersect(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOp(k1, k2)(_ intersect _).map(exactCount)

      override def aNotb(k1: Key, k2: Key): F[CardinalityEstimate] =
        setOp(k1, k2)(_ diff _).map(exactCount)

      private def setOp(k1: Key, k2: Key)(
          op: (Set[Identifier], Set[Identifier]) => Set[Identifier]
      ): F[Set[Identifier]] =
        for {
          set1 <- state.get(k1, Set.empty[Identifier])
          set2 <- state.get(k2, Set.empty[Identifier])
        } yield op(set1, set2)

      private def exactCount(set: Set[Identifier]): CardinalityEstimate = {
        val exactCount = set.size.toDouble
        CardinalityEstimate(exactCount, exactCount, exactCount)
      }
    }
}
