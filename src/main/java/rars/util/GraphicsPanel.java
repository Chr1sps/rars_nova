package rars.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Class that represents the panel for visualizing and animating memory reference patterns.
 */
public final class GraphicsPanel extends JPanel {
    private final int[][] colors;
    private final int columns, rows;
    private final @NotNull Dimension size;
    
    public GraphicsPanel(final int columns, final int rows) {
        super();
        this.colors = new int[rows][columns];
        this.columns = columns;
        this.rows = rows;
        this.size = new Dimension(columns, rows);
    }

    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

    @Override
    public Dimension getMaximumSize() {
        return this.size;
    }

    @Override
    public Dimension getMinimumSize() {
        return this.size;
    }

    public void paintPixel(final int row, final int col, final int color) {
        this.colors[row][col] = color;
        this.repaint(col, this.rows - row - 1, 1, 1);
    }

    @Override
    public void paint(final @NotNull Graphics g) {
        // override default paint method to assure display updated correctly every time
        // the panel is repainted.
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < this.columns; col++) {
                final var color = this.colors[row][col];
                g.setColor(new Color(color));
                g.fillRect(col, this.rows - row - 1, 1, 1);
            }
        }
    }
}
