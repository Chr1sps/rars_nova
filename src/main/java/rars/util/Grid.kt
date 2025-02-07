package rars.util

import java.awt.Color

/**
 * Represents grid of colors
 */
class Grid(@JvmField val rows: Int, @JvmField val columns: Int) {
    @JvmField
    val grid: Array<Array<Color>> = Array<Array<Color>>(rows) {
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
