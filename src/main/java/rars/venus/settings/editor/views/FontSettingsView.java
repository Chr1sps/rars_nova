package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.util.EditorFontUtils;
import rars.util.FontWeight;

import javax.swing.*;
import java.awt.*;

public final class FontSettingsView extends JPanel {
    private static final @NotNull JLabel
        FONT_LABEL = new JLabel("Font family"),
        WEIGHT_LABEL = new JLabel("Font weight"),
        SIZE_LABEL = new JLabel("Font size"),
        LIGATURES_LABEL = new JLabel("Ligatures");

    public final @NotNull JSpinner fontSizeSpinner;
    public final @NotNull JComboBox<@NotNull String> fontSelector;
    public final @NotNull JComboBox<@NotNull FontWeight> fontWeightSelector;
    public final @NotNull JCheckBox ligaturesCheckbox;

    public FontSettingsView() {
        super();
        this.setLayout(new GridBagLayout());
        final var gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        // font family
        gbc.gridy = 0;
        gbc.gridx = 0;
        this.add(FONT_LABEL, gbc);
        final var fontFamilies = EditorFontUtils.getAllFamilies();
        this.fontSelector = new JComboBox<>(fontFamilies);
        this.fontSelector.setEditable(false);
        this.fontSelector.setMaximumRowCount(20);
        gbc.gridx = 1;
        this.add(fontSelector, gbc);

        // font size
        gbc.gridy = 1;
        gbc.gridx = 0;
        this.add(SIZE_LABEL, gbc);
        final var fontSizeModel = new SpinnerNumberModel(
            12,
            1,
            100,
            1);
        this.fontSizeSpinner = new JSpinner(fontSizeModel);
        this.fontSizeSpinner.setToolTipText("Current font size in points.");
        gbc.gridx = 1;
        this.add(fontSizeSpinner, gbc);

        // font weight
        gbc.gridy = 2;
        gbc.gridx = 0;
        this.add(WEIGHT_LABEL, gbc);
        this.fontWeightSelector = new JComboBox<>(FontWeight.values());
        this.fontWeightSelector.setEditable(false);
        this.fontWeightSelector.setMaximumRowCount(FontWeight.values().length);
        gbc.gridx = 1;
        this.add(fontWeightSelector, gbc);


        // ligatures
        gbc.gridy = 3;
        gbc.gridx = 0;
        this.add(LIGATURES_LABEL, gbc);
        this.ligaturesCheckbox = new JCheckBox();
        this.ligaturesCheckbox.setToolTipText("Enable or disable ligatures.");
        gbc.gridx = 1;
        this.add(ligaturesCheckbox, gbc);
    }
}
