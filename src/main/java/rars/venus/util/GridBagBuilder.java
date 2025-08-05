package rars.venus.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Small helper to reduce GridBagLayout boilerplate for simple settings views.
 * Supports common two-column rows (label | control) and adding a filler.
 */
public final class GridBagBuilder {
    private final @NotNull JPanel panel;
    private final @NotNull GridBagConstraints gbc;

    public GridBagBuilder(final @NotNull JPanel panel) {
        this.panel = panel;
        if (!(panel.getLayout() instanceof GridBagLayout)) {
            panel.setLayout(new GridBagLayout());
        }
        this.gbc = new GridBagConstraints();
        // Reasonable defaults
        this.gbc.insets = new Insets(5, 5, 5, 5);
        this.gbc.anchor = GridBagConstraints.WEST;
        this.gbc.fill = GridBagConstraints.NONE;
        this.gbc.weightx = 0.0;
        this.gbc.weighty = 0.0;
        this.gbc.gridwidth = 1;
        this.gbc.gridheight = 1;
    }

    public @NotNull GridBagBuilder withInsets(final @NotNull Insets insets) {
        this.gbc.insets = insets;
        return this;
    }

    public @NotNull GridBagBuilder withAnchor(final int anchor) {
        this.gbc.anchor = anchor;
        return this;
    }

    public @NotNull GridBagBuilder withFill(final int fill) {
        this.gbc.fill = fill;
        return this;
    }

    public @NotNull GridBagBuilder add(final int x, final int y, final double weightx, final double weighty,
                                       final int fill, final @NotNull JComponent comp) {
        this.gbc.gridx = x;
        this.gbc.gridy = y;
        this.gbc.weightx = weightx;
        this.gbc.weighty = weighty;
        this.gbc.fill = fill;
        this.panel.add(comp, this.gbc);
        return this;
    }

    /**
     * Add a common two-column row: label in column 0 (no stretch), control in column 1 (horizontal stretch).
     */
    public @NotNull GridBagBuilder addLabelAndField(final int row,
                                                    final @NotNull JComponent label,
                                                    final @NotNull JComponent field) {
        // label
        add(0, row, 0.0, 0.0, GridBagConstraints.NONE, label);
        // field stretches horizontally
        add(1, row, 1.0, 0.0, GridBagConstraints.HORIZONTAL, field);
        return this;
    }

    /**
     * Add a filler/glue row that pushes content to the top. Call after adding all rows.
     * colSpan denotes number of columns in the grid.
     */
    public @NotNull GridBagBuilder addFiller(final int row, final int colSpan) {
        this.gbc.gridx = 0;
        this.gbc.gridy = row;
        this.gbc.gridwidth = colSpan;
        this.gbc.weightx = 1.0;
        this.gbc.weighty = 1.0;
        this.gbc.fill = GridBagConstraints.BOTH;
        this.panel.add(Box.createGlue(), this.gbc);
        // reset gridwidth for future calls
        this.gbc.gridwidth = 1;
        this.gbc.weighty = 0.0;
        return this;
    }
}
