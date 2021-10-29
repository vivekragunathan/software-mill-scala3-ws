package rental

import scala.util.Random

def randomId(count: Int = 10): String =
  (for (i <- 1 to count) yield Random.nextPrintableChar()).mkString
