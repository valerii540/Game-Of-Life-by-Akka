package game

import game.Response.MatrixOfStates
import game.cellADT.{Alive, Dead, State}

final case class Response(epoch: Long, matrix: MatrixOfStates)

object Response {
  type MatrixOfStates = IndexedSeq[IndexedSeq[State]]

  def asJson(response: Response): String = {
    val m = response.matrix
      .map(row =>
        s"[${row
          .map {
            case Alive => 1
            case Dead  => 0
          }.mkString(",")}]\n"
      ).mkString(",")

    s"""{"e": ${response.epoch}, "m":[${'\n'} $m]}"""
  }
}
