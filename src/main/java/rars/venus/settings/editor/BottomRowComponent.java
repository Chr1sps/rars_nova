package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class BottomRowComponent extends JPanel {
    public @NotNull JButton applyButton, applyAndCloseButton, cancelButton;

    public BottomRowComponent() {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.applyButton = new JButton("Apply");
        this.applyAndCloseButton = new JButton("Apply and close");
        this.cancelButton = new JButton("Cancel");

        this.add(Box.createHorizontalGlue());
        this.add(this.cancelButton);
        this.add(Box.createHorizontalStrut(10));
        this.add(this.applyButton);
        this.add(this.applyAndCloseButton);
    }
}
