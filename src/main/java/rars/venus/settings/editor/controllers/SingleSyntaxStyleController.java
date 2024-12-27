package rars.venus.settings.editor.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.SyntaxStyleView;

import javax.swing.*;
import java.awt.*;

public final class SingleSyntaxStyleController {
    private static final @NotNull Logger LOGGER = LogManager.getLogger();

    private final @NotNull TokenSettingKey tokenSettingKey;
    private final @NotNull SyntaxStyleView view;
    private final @NotNull TextEditingArea textArea;
    private @NotNull Color foregroundColor, backgroundColor;
    private boolean useForeground, useBackground, isBold, isItalic, isUnderline;

    public SingleSyntaxStyleController(
        final @NotNull TokenSettingKey tokenSettingKey,
        final @NotNull SyntaxStyleView view,
        final @NotNull TextEditingArea textArea
    ) {
        this.tokenSettingKey = tokenSettingKey;
        this.view = view;
        this.textArea = textArea;
        loadValuesFromSettings();
    }

    private void loadValuesFromSettings() {
        // TODO: rewrite the theme settings to use the new token setting keys
    }

    /// Sets the [SyntaxStyleView] to work on the current [TokenSettingKey] style.
    public void setAsCurrentStyle() {
        if (foregroundColor == null) {
            view.useForeground.setSelected(false);
            // TODO: use the default foreground color from the theme settings
            view.foregroundColorButton.setColor(Color.BLACK);
        } else {
            view.useForeground.setSelected(true);
            view.foregroundColorButton.setColor(foregroundColor);
        }

        if (backgroundColor == null) {
            view.useForeground.setSelected(false);
            // TODO: use the default foreground color from the theme settings
            view.foregroundColorButton.setColor(Color.WHITE);
        } else {
            view.useForeground.setSelected(true);
            view.foregroundColorButton.setColor(backgroundColor);
        }
        view.isBold.setSelected(isBold);
        view.isItalic.setSelected(isItalic);
        view.isUnderline.setSelected(isUnderline);
    }

    private void setChangeListeners() {
        this.removeChangeListeners(view.useForeground);
        this.removeChangeListeners(view.foregroundColorButton);
        view.useForeground.addChangeListener(event -> {
            useForeground = view.useForeground.isSelected();
            final @NotNull Color newColor;
            if (useForeground) {
                newColor = foregroundColor;
            } else {
                // TODO: use the default foreground color from the theme settings
                newColor = Color.BLACK;
            }
            // TODO: translate the settings key to the appropriate token types
            // TODO: apply the new color for appropriate token types in the text area
        });
        view.foregroundColorButton.addChangeListener(event -> {
            if (useForeground) {
                // TODO: translate the settings key to the appropriate token types
                // TODO: apply the new color for appropriate token types in the text area
            }
        });

        this.removeChangeListeners(view.useBackground);
        this.removeChangeListeners(view.backgroundColorButton);

        this.removeChangeListeners(view.isBold);
        this.removeChangeListeners(view.isItalic);
        this.removeChangeListeners(view.isUnderline);
    }

    private void removeChangeListeners(final @NotNull AbstractButton button) {
        final var listeners = button.getChangeListeners();
        if (listeners.length == 0) {
            return;
        }
        if (listeners.length > 1) {
            LOGGER.warn(
                "More than one change listener found for button {} in {} (settings key: {}).",
                button,
                SingleSyntaxStyleController.class,
                tokenSettingKey);
        }
        for (final var listener : listeners) {
            button.removeChangeListener(listener);
        }
    }
}
