package rars.util

import java.awt.Color

/**
 * Represents grid of colors
 */
class Grid(val rows: Int, val columns: Int) {
    val grid = Array<Array<Color>>(rows) {
        Array<Color>(columns) { Color.BLACK }
    }

    fun setColor(row: Int, column: Int, color: Color) {
        this.grid[row][column] = color
    }

    /**
     * Just set all grid elements to black.
     */
    fun reset() {
        grid.forEach { row -> row.fill(Color.BLACK) }
    }
}
