package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TokenStyle;
import rars.venus.settings.editor.ColorPickerButton;
import rars.venus.settings.editor.OptionSection;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class SyntaxStyleView extends JPanel {

    public final @NotNull JCheckBox isBold, isItalic, isUnderline, useForeground, useBackground;
    public final @NotNull ColorPickerButton foregroundColorButton, backgroundColorButton;

    public SyntaxStyleView() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // foreground
        final var foregroundSection = new OptionSection("Foreground", false, Color.BLACK);
        this.useForeground = Objects.requireNonNull(foregroundSection.checkBox);
        this.foregroundColorButton = Objects.requireNonNull(foregroundSection.colorPickerButton);

        // background
        final var backgroundSection = new OptionSection("Background", false, Color.WHITE);
        this.useBackground = Objects.requireNonNull(backgroundSection.checkBox);
        this.backgroundColorButton = Objects.requireNonNull(backgroundSection.colorPickerButton);

        // upper row
        final var upperRow = buildRow(true, foregroundSection, backgroundSection);
        this.add(upperRow);
        this.add(Box.createVerticalStrut(10));

        // bold
        final var boldSection = new OptionSection("Bold", false, null);
        this.isBold = Objects.requireNonNull(boldSection.checkBox);

        // italic
        final var italicSection = new OptionSection("Italic", false, null);
        this.isItalic = Objects.requireNonNull(italicSection.checkBox);

        // underline
        final var underlineSection = new OptionSection("Underline", false, null);
        this.isUnderline = Objects.requireNonNull(underlineSection.checkBox);

        // bottom row
        final var bottomRow = buildRow(true, boldSection, italicSection, underlineSection);
        this.add(bottomRow, BorderLayout.SOUTH);
    }

    public static @NotNull JPanel buildRow(final boolean addMargins, final @NotNull JComponent... sections) {
        final var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        if (addMargins) {
            panel.add(Box.createHorizontalGlue());
        }
        Arrays.stream(sections)
            .flatMap(s -> Stream.of(Box.createHorizontalGlue(), s))
            .skip(1)
            .forEach(panel::add);
        if (addMargins) {
            panel.add(Box.createHorizontalGlue());
        }
        return panel;
    }

    public void setFromTokenStyle(final @NotNull TokenStyle style) {
        final var foreground = style.foreground();
        if (foreground != null) {
            this.useForeground.setSelected(true);
            this.foregroundColorButton.setColor(foreground);
        } else {
            this.useForeground.setSelected(false);
        }

        final var background = style.background();
        if (background != null) {
            this.useBackground.setSelected(true);
            this.backgroundColorButton.setColor(background);
        } else {
            this.useBackground.setSelected(false);
        }

        this.isBold.setSelected(style.isBold());
        this.isItalic.setSelected(style.isItalic());
        this.isUnderline.setSelected(style.isUnderline());
    }

    public @NotNull TokenStyle getTokenStyle() {
        return new TokenStyle(
            this.useForeground.isSelected() ? this.foregroundColorButton.getColor() : null,
            this.useBackground.isSelected() ? this.backgroundColorButton.getColor() : null,
            this.isBold.isSelected(),
            this.isItalic.isSelected(),
            this.isUnderline.isSelected()
        );
    }
}
