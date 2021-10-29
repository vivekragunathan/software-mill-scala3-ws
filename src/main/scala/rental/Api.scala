package rental

import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml.*
import sttp.tapir.swagger.SwaggerUI
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI
import sttp.tapir.json.circe.*

import scala.concurrent.ExecutionContext
import scala.util.Random

given IORuntime = IORuntime.global

import scala.concurrent.ExecutionContext

def testTapir() =
  val helloWorld: Endpoint[String, Unit, String, Any] =
    endpoint.get
      .in("hello")
      .in(query[String]("name"))
      .out(stringBody)

  val rentTent: ServerEndpoint[Int, RentalError, RentalItem, Any, IO] =
    endpoint.post
      .in("rent" / "tent")
      .in(query[Int]("people"))
      .errorOut(jsonBody[RentalError])
      .out(jsonBody[RentalItem])
      .serverLogic(people =>
        IO {
          if (people > 6) then RentalError.OutOfStock.asLeft
          else
            RentalItem(
              randomId(),
              Rental.Tent(people, hasVestibule = Random.nextBoolean())
            ).asRight
        }
      )

  val docs: String =
    OpenAPIDocsInterpreter()
      .toOpenAPI(helloWorld :: rentTent.endpoint :: Nil, "Hello, World!", "1.0")
      .toYaml

  val helloWorldRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(helloWorld)(name => s"Hello $name".asRight.pure[IO])

  val swaggerUIRoute = Http4sServerInterpreter[IO]().toRoutes(SwaggerUI[IO](docs))
  val rentalRoutes   = Http4sServerInterpreter[IO]().toRoutes(rentTent)

  BlazeServerBuilder[IO](ExecutionContext.Implicits.global)
    .bindHttp(8080, "localhost")
    .withHttpApp(Router("/" -> (helloWorldRoutes <+> rentalRoutes <+> swaggerUIRoute)).orNotFound)
    .resource
    .use(_ => IO.never)
    .unsafeRunSync()
