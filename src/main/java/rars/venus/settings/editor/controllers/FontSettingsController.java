package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.FontSettings;
import rars.util.FontWeight;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.FontSettingsView;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

public final class FontSettingsController {
    private final @NotNull FontSettingsView view;
    private final @NotNull TextEditingArea textArea;
    private final @NotNull FontSettings settings;

    private boolean ligaturesEnabled;
    private int fontSize;
    private @NotNull String fontFamily;
    private @NotNull FontWeight fontWeight;

    public FontSettingsController(
        final @NotNull FontSettingsView view,
        final @NotNull TextEditingArea textArea,
        final @NotNull FontSettings settings
    ) {
        this.view = view;
        this.textArea = textArea;
        this.settings = settings;
        resetButtonValues();
        initializeControls();
    }

    private void initializeControls() {
        view.fontSelector.setSelectedItem(this.fontFamily);
        view.fontSelector.addItemListener(e -> {
            final var selected = (String) view.fontSelector.getSelectedItem();
            if (selected == null) return;
            this.fontFamily = selected;
            final var currentFont = textArea.getFont();
            final var derivedFont = new Font(this.fontFamily, currentFont.getStyle(), currentFont.getSize());
            textArea.setFont(derivedFont);
        });
        view.ligaturesCheckbox.setSelected(this.ligaturesEnabled);
        view.ligaturesCheckbox.addItemListener(e -> {
            this.ligaturesEnabled = view.ligaturesCheckbox.isSelected();
            final var currentFont = textArea.getFont();
            final var transformMap = Map.of(TextAttribute.LIGATURES, this.ligaturesEnabled ? 1 : 0);
            final var derivedFont = currentFont.deriveFont(transformMap);
            textArea.setFont(derivedFont);
        });
        view.fontSizeSpinner.setValue(this.fontSize);
        view.fontSizeSpinner.addChangeListener(e -> {
            this.fontSize = (int) view.fontSizeSpinner.getValue();
            final var currentFont = textArea.getFont();
            final var derivedFont = new Font(currentFont.getName(), currentFont.getStyle(), this.fontSize);
            textArea.setFont(derivedFont);
        });
        view.fontWeightSelector.setSelectedItem(this.fontWeight);
        view.fontWeightSelector.addItemListener(e -> {
            final var selected = (FontWeight) view.fontWeightSelector.getSelectedItem();
            if (selected == null) return;
            this.fontWeight = selected;
            final var currentFont = textArea.getFont();
            final var transformMap = Map.of(TextAttribute.WEIGHT, this.fontWeight.weight);
            final var derivedFont = currentFont.deriveFont(transformMap);
            textArea.setFont(derivedFont);
        });
    }

    public void applySettings() {
        this.settings.setFontFamily(this.fontFamily);
        this.settings.setFontSize(this.fontSize);
        this.settings.isLigaturized = this.ligaturesEnabled;
        this.settings.fontWeight = this.fontWeight;
        this.settings.saveSettingsToPreferences();
    }

    public void resetButtonValues() {
        this.ligaturesEnabled = this.settings.isLigaturized;
        this.fontFamily = this.settings.getFontFamily();
        this.fontSize = this.settings.getFontSize();
        this.fontWeight = this.settings.fontWeight;
    }
}
