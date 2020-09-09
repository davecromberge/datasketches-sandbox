package datasketches.sandbox.http.routes

import cats.effect.{Concurrent, Sync}
import cats.implicits._
import datasketches.sandbox.domain.{Identifier, Key}
import datasketches.sandbox.sketch.DistinctCount
import io.circe.generic.auto._
import fs2.text
import fs2.Chunk
import org.http4s.EntityDecoder.multipart
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Part
import org.http4s.server.Router

final class DistinctCountRoutes[F[_]: Sync: Concurrent](
    exact: DistinctCount[F],
    sketch: DistinctCount[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/distinct/count"

  object ExactQueryParam extends FlagQueryParamMatcher("exact")

  private lazy val countRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / key :? ExactQueryParam(exactMode) =>
        Ok(
          if (exactMode) exact.estimate(Key(key)) else sketch.estimate(Key(key))
        )
      case GET -> Root / key1 / "union" / key2 :? ExactQueryParam(exactMode) =>
        Ok(
          if (exactMode) exact.union(Key(key1), Key(key2))
          else sketch.union(Key(key1), Key(key2))
        )
      case GET -> Root / key1 / "intersect" / key2 :? ExactQueryParam(
            exactMode
          ) =>
        Ok(
          if (exactMode) exact.intersect(Key(key1), Key(key2))
          else sketch.intersect(Key(key1), Key(key2))
        )
      case GET -> Root / key1 / "anotb" / key2 :? ExactQueryParam(exactMode) =>
        Ok(
          if (exactMode) exact.aNotb(Key(key1), Key(key2))
          else sketch.aNotb(Key(key1), Key(key2))
        )
      case PUT -> Root / key / id :? ExactQueryParam(exactMode) =>
        val chunk = Chunk.singleton(Identifier(id))
        val k = Key(key)
        Accepted(
          if (exactMode) exact.set(chunk, k) else sketch.set(chunk, k)
        )
      case DELETE -> Root / key :? ExactQueryParam(exactMode) =>
        Ok(
          if (exactMode) exact.clear(Key(key)) else sketch.clear(Key(key))
        )
      case req @ POST -> Root / key :? ExactQueryParam(exactMode) =>
        val fa =
          if (exactMode) exact.set(_, Key(key)) else sketch.set(_, Key(key))

        req.decodeWith(multipart[F], strict = true) { multipart =>
          def filterFileTypes(part: Part[F]): Boolean =
            part.headers.toList.exists(_.value.contains("filename"))

          Accepted(
            multipart.parts
              .filter(filterFileTypes)
              .traverse { part =>
                part.bodyText
                  .through(text.lines)
                  .chunks
                  .evalMap(c => fa(c.map(Identifier)))
              }
              .compile
              .drain
              .void
          )
        }
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> countRoutes
  )
}
