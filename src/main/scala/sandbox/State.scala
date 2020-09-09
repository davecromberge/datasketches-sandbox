package datasketches.sandbox

import cats.effect.Sync
import cats.Functor
import cats.syntax.functor._
import datasketches.sandbox.domain.Key
import io.chrisdavenport.mapref.MapRef

trait State[F[_], A] {
  def get(key: Key): F[Option[A]]
  def get(key: Key, default: => A): F[A]
  def set(key: Key, value: A): F[Unit]
  def clear(key: Key): F[Unit]
}

final class MapRefState[F[_]: Functor, A](ref: MapRef[F, Key, Option[A]])
    extends State[F, A] {
  override def get(key: Key): F[Option[A]] = ref(key).get
  override def get(key: Key, default: => A): F[A] =
    ref(key).get.map(_.getOrElse(default))
  override def set(key: Key, value: A): F[Unit] = ref(key).set(Some(value))
  override def clear(key: Key): F[Unit] = ref(key).set(Option.empty[A])
}

object MapRefState {
  @inline def apply[F[_]: Sync, A]: F[MapRefState[F, A]] =
    MapRef.ofScalaConcurrentTrieMap[F, Key, A].map(r => new MapRefState(r))
}
