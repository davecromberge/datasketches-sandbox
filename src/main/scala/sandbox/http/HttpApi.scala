package datasketches.sandbox.http

import cats.effect._
import datasketches.sandbox.Environment
import datasketches.sandbox.http.routes._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._

import scala.concurrent.duration._

object HttpApi {
  def make[F[_]: Concurrent: Timer](
      environment: Environment[F]
  ): F[HttpApi[F]] =
    Sync[F].delay(
      new HttpApi[F](environment)
    )
}

final class HttpApi[F[_]: Concurrent: Timer] private (
    environment: Environment[F]
) {
  private val adminRoutes = new AdminRoutes[F]

  private val distinctCountRoutes =
    new DistinctCountRoutes[F](environment.exactCount, environment.sketchCount).routes

  private val openRoutes: HttpRoutes[F] =
    distinctCountRoutes

  private val routes: HttpRoutes[F] = Router(
    "" -> adminRoutes.routes,
    version.v1 -> openRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    http: HttpRoutes[F] =>
      AutoSlash(http)
  }.andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    }
    .andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }

  private val loggers: HttpApp[F] => HttpApp[F] = { http: HttpApp[F] =>
    RequestLogger.httpApp(true, false)(http)
  }.andThen { http: HttpApp[F] =>
    ResponseLogger.httpApp(true, false)(http)
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}
