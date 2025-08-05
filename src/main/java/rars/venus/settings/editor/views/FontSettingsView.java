package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.util.FontUtilities;
import rars.util.FontWeight;
import rars.venus.util.GridBagBuilder;

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
        super(new GridBagLayout());
        final var builder = new GridBagBuilder(this)
            .withInsets(new Insets(5, 5, 5, 5));

        // font family
        final var fontFamilies = FontUtilities.allFontFamilies;
        this.fontSelector = new JComboBox<>(fontFamilies);
        this.fontSelector.setEditable(false);
        this.fontSelector.setMaximumRowCount(20);
        builder.addLabelAndField(0, FONT_LABEL, this.fontSelector);

        // font size
        final var fontSizeModel = new SpinnerNumberModel(
            12,
            1,
            100,
            1
        );
        this.fontSizeSpinner = new JSpinner(fontSizeModel);
        this.fontSizeSpinner.setToolTipText("Current font size in points.");
        builder.addLabelAndField(1, SIZE_LABEL, this.fontSizeSpinner);

        // font weight
        this.fontWeightSelector = new JComboBox<>(FontWeight.values());
        this.fontWeightSelector.setEditable(false);
        this.fontWeightSelector.setMaximumRowCount(FontWeight.values().length);
        builder.addLabelAndField(2, WEIGHT_LABEL, this.fontWeightSelector);

        // ligatures
        this.ligaturesCheckbox = new JCheckBox();
        this.ligaturesCheckbox.setToolTipText("Enable or disable ligatures.");
        builder.addLabelAndField(3, LIGATURES_LABEL, this.ligaturesCheckbox);

        // filler to push content to top
        builder.addFiller(4, 2);
    }
}
