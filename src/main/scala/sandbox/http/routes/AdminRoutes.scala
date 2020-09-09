package datasketches.sandbox.http.routes

import cats.{Applicative, Defer}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl

final class AdminRoutes[F[_]: Applicative: Defer] extends Http4sDsl[F] {
  private val adminRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "ping" => Ok("pong")
  }
  val routes: HttpRoutes[F] = adminRoutes
}
