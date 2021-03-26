package game

import game.Response.MatrixOfStates

final case class Response(epoch: Long, matrix: MatrixOfStates)

object Response {
  type MatrixOfStates = Array[Array[Boolean]]

  def asJson(response: Response): String = {
    val m = response.matrix
      .map(row =>
        s"[${row
          .map {
            case true  => 1
            case false => 0
          }
          .mkString(",")}]\n"
      )
      .mkString(",")

    s"""{"e": ${response.epoch}, "m":[${'\n'} $m]}"""
  }
}
