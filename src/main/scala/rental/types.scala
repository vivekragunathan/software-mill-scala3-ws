package rental

import cats.effect.unsafe.IORuntime
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

enum Rental derives Encoder.AsObject, Decoder, Schema:
  case Tent(people: Int, hasVestibule: Boolean)
  case Van(kind: VanKind, length: Units.Meters)
  case House(bedrooms: Int, location: (Double, Double))

enum VanKind:
  case Medium, Large, Pickup, Minibus, Camper

given Encoder[VanKind]         = Encoder.encodeString.contramap(_.toString)
given Schema[VanKind]          = Schema.string
given Schema[(Double, Double)] = Schema.schemaForArray[Double].as[(Double, Double)]

extension (r: Rental)
  def capacity: Int = r match
    case Rental.Tent(p, _)  => p
    case Rental.House(b, _) => b * 2
    case _: Rental.Van => 4

object Units:
  opaque type Meters = Double

  object Meters:
    def apply(d: Double): Meters = d
    given Encoder[Meters] = Encoder.encodeDouble.contramap(identity)
    given Decoder[Meters] = Decoder.decodeDouble.map(Meters(_))
    given Schema[Meters]  = Schema.schemaForDouble

  extension (u: Meters)
    def show: String =
      val m  = Math.floor(u).toInt
      val cm = Math.floor(u * 100).toInt % 100
      s"${m}m${cm}cms"

enum RentalError derives Encoder.AsObject, Decoder, Schema:
  case OutOfStock, TooManyPeople

final case class RentalItem(id: String, item: Rental) derives Encoder.AsObject, Decoder, Schema
