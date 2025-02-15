package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class ColorPickerButton extends JButton {
    private @NotNull Color color;

    public ColorPickerButton(final Color color) {
        super("Pick Color");
        this.setColor(color);
        this.addActionListener((event) -> {
            final var result = JColorChooser.showDialog(null, "Choose color", this.color, false);
            if (result != null) {
                this.setColor(result);
            }
        });
    }

    private static Color getBestForegroundForBackground(final @NotNull Color background) {
        final var r = background.getRed();
        final var g = background.getGreen();
        final var b = background.getBlue();

        // Calculate luminance
        final var luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        // Return white or black based on luminance
        return luminance > 127.5 ? Color.BLACK : Color.WHITE;
    }

    public @NotNull Color getColor() {
        return this.color;
    }

    public void setColor(final @NotNull Color color) {
        this.color = color;
        final var foreground = getBestForegroundForBackground(color);
        this.setBackground(color);
        this.setForeground(foreground);
        this.setText(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
    }
}
