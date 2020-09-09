package datasketches.sandbox

import cats.effect._
import datasketches.sandbox.http.HttpApi
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      env <- Environment[IO]
      api <- HttpApi.make[IO](env)
      _ <- BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(api.httpApp)
        .serve
        .compile
        .drain
    } yield ExitCode.Success
}
