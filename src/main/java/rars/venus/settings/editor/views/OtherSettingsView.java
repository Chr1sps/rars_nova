package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.venus.Editor;
import rars.venus.util.GridBagBuilder;

import javax.swing.*;
import java.awt.*;

import static rars.Globals.OTHER_SETTINGS;

public final class OtherSettingsView extends JPanel {
    private static final @NotNull JLabel BLINK_LABEL = new JLabel("Caret blink rate (ms, 0 to disable)");
    private static final @NotNull JLabel TAB_LABEL = new JLabel("Tab size");

    public final @NotNull JSpinner blinkRateSpinner, tabSizeSpinner;

    public OtherSettingsView() {
        super(new GridBagLayout());
        final var builder = new GridBagBuilder(this);

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

        // Rows
        builder.addLabelAndField(0, BLINK_LABEL, this.blinkRateSpinner);
        builder.addLabelAndField(1, TAB_LABEL, this.tabSizeSpinner);

        // Filler to push content to top without stretching rows
        builder.addFiller(2, 2);
    }

    private static void constrainHeight(final @NotNull JComponent c, final int h) {
        final Dimension pref = c.getPreferredSize();
        final var sized = new Dimension(pref.width, h);
        c.setPreferredSize(sized);
        c.setMinimumSize(new Dimension(0, h));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }
}
