package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.util.EditorFontUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Vector;

public final class FontPickerPanel extends JPanel {
    private static final @NotNull JLabel
            FONT_LABEL = new JLabel("Font family"),
            WEIGHT_LABEL = new JLabel("Font weight"),
            SIZE_LABEL = new JLabel("Font size"),
            LIGATURES_LABEL = new JLabel("Ligatures");

    private static final @NotNull
    @Unmodifiable List<FontWeightEntry> WEIGHTS = List.of(
            new FontWeightEntry("Extra light", TextAttribute.WEIGHT_EXTRA_LIGHT),
            new FontWeightEntry("Light", TextAttribute.WEIGHT_LIGHT),
            new FontWeightEntry("Demi-light", TextAttribute.WEIGHT_DEMILIGHT),
            new FontWeightEntry("Regular", TextAttribute.WEIGHT_REGULAR),
            new FontWeightEntry("Semi-bold", TextAttribute.WEIGHT_SEMIBOLD),
            new FontWeightEntry("Medium", TextAttribute.WEIGHT_MEDIUM),
            new FontWeightEntry("Demi-bold", TextAttribute.WEIGHT_DEMIBOLD),
            new FontWeightEntry("Bold", TextAttribute.WEIGHT_BOLD),
            new FontWeightEntry("Heavy", TextAttribute.WEIGHT_HEAVY),
            new FontWeightEntry("Extra bold", TextAttribute.WEIGHT_EXTRABOLD),
            new FontWeightEntry("Ultra bold", TextAttribute.WEIGHT_ULTRABOLD)
    );
    private final @NotNull JSpinner fontSizeSpinner;
    private final @NotNull JComboBox<String> fontSelector;
    private final @NotNull JComboBox<FontWeightEntry> fontWeightSelector;
    private final @NotNull JCheckBox ligaturesCheckbox;

    public FontPickerPanel() {
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
        this.fontWeightSelector = new JComboBox<>(new Vector<>(WEIGHTS));
        this.fontWeightSelector.setEditable(false);
        this.fontWeightSelector.setMaximumRowCount(WEIGHTS.size());
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

    public @NotNull JComboBox<String> getFontSelector() {
        return fontSelector;
    }

    public @NotNull JComboBox<FontWeightEntry> getFontWeightSelector() {
        return fontWeightSelector;
    }

    public @NotNull JSpinner getFontSizeSpinner() {
        return fontSizeSpinner;
    }

    public @NotNull JCheckBox getLigaturesCheckbox() {
        return ligaturesCheckbox;
    }

    public record FontWeightEntry(String name, float weight) {
        @Override
        public String toString() {
            return name;
        }
    }
}
