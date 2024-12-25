package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static rars.settings.Settings.OTHER_SETTINGS;
import static rars.venus.settings.editor.SyntaxStylePickerPanel.buildRow;

public final class OtherSettingsPanel extends JPanel {
    public @NotNull JSpinner blinkRateSpinner, tabSizeSpinner;
    
    public OtherSettingsPanel() {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final var blinkRatePicker = new GenericOptionSection<>(
            "Caret blink rate (ms, 0 to disable)",
            new JSpinner(new SpinnerNumberModel(OTHER_SETTINGS.getCaretBlinkRate(), 0, 1000, 1))
        );
        this.blinkRateSpinner = blinkRatePicker.component; 
        final var tabSizePicker = new GenericOptionSection<>(
            "Tab size",
            new JSpinner(new SpinnerNumberModel(OTHER_SETTINGS.getEditorTabSize(), 1, 40, 1))
        );
        this.tabSizeSpinner = tabSizePicker.component;
        this.add(buildRow(true, blinkRatePicker, tabSizePicker));
    }
}
