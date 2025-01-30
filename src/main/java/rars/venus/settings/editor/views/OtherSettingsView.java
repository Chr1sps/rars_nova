package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.settings.OtherSettingsImpl;
import rars.venus.Editor;
import rars.venus.settings.editor.GenericOptionSection;

import javax.swing.*;

import static rars.venus.settings.editor.views.SyntaxStyleView.buildRow;

public final class OtherSettingsView extends JPanel {
    public final @NotNull JSpinner blinkRateSpinner, tabSizeSpinner;

    public OtherSettingsView(final @NotNull OtherSettingsImpl otherSettings) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final var blinkRateModel = new SpinnerNumberModel(
            otherSettings.getCaretBlinkRate(),
            Editor.MIN_BLINK_RATE,
            Editor.MAX_BLINK_RATE,
            1
        );
        final var blinkRatePicker = new GenericOptionSection<>(
            "Caret blink rate (ms, 0 to disable)",
            new JSpinner(blinkRateModel)
        );
        this.blinkRateSpinner = blinkRatePicker.component;
        final var tabSizeModel = new SpinnerNumberModel(
            otherSettings.getEditorTabSize(),
            Editor.MIN_TAB_SIZE,
            Editor.MAX_TAB_SIZE,
            1
        );
        final var tabSizePicker = new GenericOptionSection<>(
            "Tab size",
            new JSpinner(tabSizeModel)
        );
        this.tabSizeSpinner = tabSizePicker.component;
        this.add(buildRow(true, blinkRatePicker, tabSizePicker));
        this.add(Box.createVerticalGlue());
    }
}
