package rars.util;

import java.awt.*;
import java.util.Arrays;

// Represents grid of colors
public final class Grid {

    public final Color[][] grid;
    public final int rows;
    public final int columns;

    public Grid(final int rows, final int columns) {
        this.grid = new Color[rows][columns];
        this.rows = rows;
        this.columns = columns;
        this.reset();
    }

    public void setColor(final int row, final int column, final Color color) {
        this.grid[row][column] = color;
    }

    // Just set all grid elements to black.
    public void reset() {
        for (final var row : this.grid) {
            Arrays.fill(row, Color.BLACK);
        }
    }
}
