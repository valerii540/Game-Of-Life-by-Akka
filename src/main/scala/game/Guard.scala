package game

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import game.cellADT.Dead
import ui.ConsoleUI

object Guard {
  val rows = 10
  val columns = 10

  def apply(): Behavior[Unit] =
    Behaviors.setup { ctx =>
      val board = IndexedSeq.tabulate(rows, columns)((r, c) => ctx.spawn(Cell(Dead), s"cell-$r-$c"))
      val consoleUI = new ConsoleUI(board, ctx.system)

      consoleUI.write()

      Behaviors.empty
    }
}
