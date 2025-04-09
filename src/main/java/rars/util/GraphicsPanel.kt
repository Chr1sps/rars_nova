package rars.util

import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

/**
 * Class that represents the panel for visualizing and animating memory reference patterns.
 */
class GraphicsPanel @JvmOverloads constructor(
    size: Dimension,
    private val grid: Grid = Grid(size.height, size.width)
) : JPanel() {
    init {
        minimumSize = size
        preferredSize = size
        maximumSize = size
    }

    override fun paint(g: Graphics) {
        for (row in 0..<grid.rows) {
            for (col in 0..<grid.columns) {
                val color = this.grid.grid[row][col]
                g.color = color
                g.fillRect(col, grid.rows - row - 1, 1, 1)
            }
        }
    }
}
