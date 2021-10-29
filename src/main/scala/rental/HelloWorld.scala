package rental

import io.circe.{Encoder, Json}
import io.circe.syntax.*
import scala.util.Random

final case class Person(name: String, age: Int) derives Encoder.AsObject

def encode(p: Person): Json =
  Json.fromFields(
    List(
      "name" -> Json.fromString(p.name),
      "age"  -> Json.fromInt(p.age)
    )
  )

def playWithCirce: Unit =
  val p = Person(Random.nextString(6), Random.nextInt(40))
  // given Encoder[Person] = Encoder.AsObject.derived[Person]
  //  println(Encoder.AsObject.derived[Person](p))
  println(p.asJson)

def testRentals =
  val rentals: List[Rental] =
    Rental.Tent(10, true) ::
      Rental.Van(VanKind.Camper, Units.Meters(5.25)) ::
      Nil

  //  rentals.foreach(r => println(r.asJson))
  rentals.foreach(r => println(r.capacity))
  println(Units.Meters(10.129).show)
