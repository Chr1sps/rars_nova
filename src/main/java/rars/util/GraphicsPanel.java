package rars.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/////////////////////////////////////////////////////////////////////////////
// Class that represents the panel for visualizing and animating memory
///////////////////////////////////////////////////////////////////////////// reference
// patterns.
public final class GraphicsPanel extends JPanel {
    private final Grid grid;

    public GraphicsPanel(final @NotNull Dimension size, final @NotNull Grid grid) {
        super();
        this.grid = grid;
        this.setMinimumSize(size);
        this.setPreferredSize(size);
        this.setMaximumSize(size);
    }

    public GraphicsPanel(final @NotNull Dimension size) {
        this(size, new Grid(size.height, size.width));
    }

    // override default paint method to assure display updated correctly every time
    // the panel is repainted.
    @Override
    public void paint(final @NotNull Graphics g) {
        for (int row = 0; row < this.grid.rows; row++) {
            for (int col = 0; col < this.grid.columns; col++) {
                final var color = this.grid.grid[row][col];
                g.setColor(color);
                g.fillRect(col, grid.rows - row - 1, 1, 1);
            }
        }
//        int upperLeftX = 0, upperLeftY = 0;
//        for (final var row : this.grid.grid) {
//            for (final var color : row) {
//                g.setColor(color);
//                g.fillRect(upperLeftX, upperLeftY, 1, 1);
//                upperLeftX += 1; // faster than multiplying
//            }
//            upperLeftX = 0;
//            upperLeftY += 1; // faster than multiplying
//        }
    }

    // Paint the color codes.
}