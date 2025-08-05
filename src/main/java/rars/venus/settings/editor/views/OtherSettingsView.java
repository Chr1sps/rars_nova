package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.venus.Editor;

import javax.swing.*;
import java.awt.*;

import static rars.Globals.OTHER_SETTINGS;

public final class OtherSettingsView extends JPanel {
    private static final @NotNull JLabel BLINK_LABEL = new JLabel("Caret blink rate (ms, 0 to disable)");
    private static final @NotNull JLabel TAB_LABEL = new JLabel("Tab size");

    public final @NotNull JSpinner blinkRateSpinner, tabSizeSpinner;

    public OtherSettingsView() {
        super(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        // Create models
        final var blinkRateModel = new SpinnerNumberModel(
            OTHER_SETTINGS.getCaretBlinkRate(),
            Editor.MIN_BLINK_RATE,
            Editor.MAX_BLINK_RATE,
            1
        );
        final var tabSizeModel = new SpinnerNumberModel(
            OTHER_SETTINGS.getEditorTabSize(),
            Editor.MIN_TAB_SIZE,
            Editor.MAX_TAB_SIZE,
            1
        );

        // Create components
        this.blinkRateSpinner = new JSpinner(blinkRateModel);
        this.tabSizeSpinner = new JSpinner(tabSizeModel);

        // Ensure spinners do not stretch vertically (keep a tidy row height)
        final int rowH = Math.max(
            new JLabel("X").getPreferredSize().height,
            Math.max(this.blinkRateSpinner.getPreferredSize().height, this.tabSizeSpinner.getPreferredSize().height)
        );
        constrainHeight(BLINK_LABEL, rowH);
        constrainHeight(TAB_LABEL, rowH);
        constrainHeight(this.blinkRateSpinner, rowH);
        constrainHeight(this.tabSizeSpinner, rowH);

        // Row 0: Blink rate
        gbc.gridy = 0;
        // column 0: label
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        this.add(BLINK_LABEL, gbc);
        // column 1: spinner (let it grow horizontally a bit)
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.blinkRateSpinner, gbc);

        // Row 1: Tab size
        gbc.gridy = 1;
        // column 0: label
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        this.add(TAB_LABEL, gbc);
        // column 1: spinner
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.tabSizeSpinner, gbc);

        // Filler to push content to top without stretching rows
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(Box.createGlue(), gbc);
    }

    private static void constrainHeight(final @NotNull JComponent c, final int h) {
        final Dimension pref = c.getPreferredSize();
        final var sized = new Dimension(pref.width, h);
        c.setPreferredSize(sized);
        c.setMinimumSize(new Dimension(0, h));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }
}
