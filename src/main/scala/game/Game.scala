package game

import scala.util.{Random, Try}

class Game(rows: Int, columns: Int) {
  private var epoch = 0L

  private val matrix: Array[Array[Boolean]] = Array.tabulate(rows, columns)((_, _) => deadOrAlive())

  private def deadOrAlive(): Boolean = if (Random.nextInt(100) < 20) true else false

  def mutate(): Response = {
    epoch += 1
    val snapshot = matrix.map(_.clone())

    for {
      row <- snapshot.indices
      col <- snapshot(row).indices
      neighbours =
        Seq(
          Try(snapshot(row - 1)(col + 1)).toOption, // TR
          Try(snapshot(row - 1)(col - 1)).toOption, // TL
          Try(snapshot(row)(col - 1)).toOption,     // L
          Try(snapshot(row)(col + 1)).toOption,     // R
          Try(snapshot(row + 1)(col + 1)).toOption, // BR
          Try(snapshot(row + 1)(col - 1)).toOption, // BL
          Try(snapshot(row - 1)(col)).toOption,     // B
          Try(snapshot(row + 1)(col)).toOption      // T
        ).flatten.count(_ == true)
    } neighbours match {
      case n if n <= 1 || n >= 4 => matrix(row)(col) = false
      case n if n == 3           => matrix(row)(col) = true
      case _                     =>
    }

    Response(epoch, matrix)
  }
}
