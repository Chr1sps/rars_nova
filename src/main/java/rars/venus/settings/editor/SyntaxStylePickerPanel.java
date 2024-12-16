package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class SyntaxStylePickerPanel extends JPanel {

    private static final @NotNull JLabel FOREGROUND_LABEL = new JLabel("Foreground");
    private static final @NotNull JLabel BACKGROUND_LABEL = new JLabel("Background");
    private static final @NotNull JLabel BOLD_LABEL = new JLabel("Bold");
    private static final @NotNull JLabel ITALIC_LABEL = new JLabel("Italic");
    private static final @NotNull JLabel UNDERLINE_LABEL = new JLabel("Underline");

    private final @NotNull JCheckBox isBold, isItalic, isUnderline, useForeground, useBackground;
    private final @NotNull ColorPickerButton foregroundColorPicker, backgroundColorPicker;

    public SyntaxStylePickerPanel() {
        this.setLayout(new GridBagLayout());
                
        final var gbc = new GridBagConstraints();
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        // foreground
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(FOREGROUND_LABEL, gbc);
        
        this.useForeground = new JCheckBox();
        gbc.gridx = 1;
        this.add(useForeground, gbc);
        
        this.foregroundColorPicker = new ColorPickerButton();
        this.useForeground.addChangeListener((event) -> {
            final var isSelected = useForeground.isSelected();
            foregroundColorPicker.setEnabled(isSelected);
        });
        gbc.gridx = 2;
        this.add(foregroundColorPicker, gbc);

        // background
        
        gbc.gridy = 1;
        gbc.gridx = 0;
        this.add(BACKGROUND_LABEL, gbc);
        
        this.useBackground = new JCheckBox();
        gbc.gridx = 1;
        this.add(useBackground, gbc);
        this.backgroundColorPicker = new ColorPickerButton();
        this.useBackground.addChangeListener((event) -> {
            final var isSelected = useBackground.isSelected();
            backgroundColorPicker.setEnabled(isSelected);
        });
        gbc.gridx = 2;
        this.add(backgroundColorPicker, gbc);

        // bold
        gbc.gridy = 2;
        gbc.gridx = 0;
        this.add(BOLD_LABEL, gbc);
        this.isBold = new JCheckBox();
        gbc.gridx = 1;
        this.add(isBold, gbc);

        // italic
        gbc.gridy = 3;
        gbc.gridx = 0;
        this.add(ITALIC_LABEL, gbc);
        this.isItalic = new JCheckBox();
        gbc.gridx = 1;
        this.add(isItalic, gbc);

        // underline
        gbc.gridy = 4;
        gbc.gridx = 0;
        this.add(UNDERLINE_LABEL, gbc);
        this.isUnderline = new JCheckBox();
        gbc.gridx = 1;
        this.add(isUnderline, gbc);
    }

    private @NotNull ColorPickerButton getForegroundColorPicker() {
        return foregroundColorPicker;
    }

    private @NotNull ColorPickerButton getBackgroundColorPicker() {
        return backgroundColorPicker;
    }

    private @NotNull JCheckBox getUseForegroundCheckbox() {
        return useForeground;
    }
    
    private @NotNull JCheckBox getUseBackgroundCheckbox() {
        return useBackground;
    }
    
    private @NotNull JCheckBox getBoldCheckbox() {
        return isBold;
    }

    private @NotNull JCheckBox getItalicCheckbox() {
        return isItalic;
    }

    private @NotNull JCheckBox getUnderlineCheckbox() {
        return isUnderline;
    }
}
