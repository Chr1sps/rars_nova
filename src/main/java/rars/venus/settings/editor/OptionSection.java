package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class OptionSection extends JPanel {
    public final @Nullable JCheckBox checkBox;
    public final @Nullable ColorPickerButton colorPickerButton;

    public OptionSection(
        final @NotNull String label,
        final @Nullable Boolean checkBoxState,
        final @Nullable Color colorPickerButtonState
    ) {
        super();
        var isFirst = true;
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(new JLabel(label));
        if (checkBoxState != null) {
            this.checkBox = new JCheckBox();
            this.checkBox.setSelected(checkBoxState);
            this.add(Box.createHorizontalStrut(10));
            isFirst = false;
            this.add(this.checkBox);
        } else {
            this.checkBox = null;
        }
        if (colorPickerButtonState != null) {
            this.colorPickerButton = new ColorPickerButton(colorPickerButtonState);
            if (checkBoxState != null) {
                this.colorPickerButton.setEnabled(checkBoxState);
                this.checkBox.addChangeListener((event) -> {
                    final var isSelected = this.checkBox.isSelected();
                    this.colorPickerButton.setEnabled(isSelected);
                });
            }
            if (isFirst) {
                this.add(Box.createHorizontalStrut(10));
            } else {
                this.add(Box.createHorizontalStrut(5));
            }
            this.add(this.colorPickerButton);
        } else {
            this.colorPickerButton = null;
        }
    }
}
