package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TokenStyle;
import rars.venus.settings.editor.views.SyntaxStyleView;

import javax.swing.event.ChangeListener;
import java.util.List;

public final class SyntaxStyleSettingsController {
    private final @NotNull SyntaxStyleView view;
    private final @NotNull EditorSettingsController parentController;
    private final @NotNull TextEditingArea textArea;
    private @NotNull TokenSettingKey currentKey;

    public SyntaxStyleSettingsController(
        final @NotNull SyntaxStyleView view,
        final @NotNull EditorSettingsController parentController,
        final @NotNull TextEditingArea textArea
    ) {
        this.view = view;
        this.parentController = parentController;
        this.textArea = textArea;
        this.currentKey = TokenSettingKey.COMMENT; // initial value
        initializeView();

        // initialize selection based on the list
        final var selected = this.view.tokenTypesList.getSelectedValue();
        if (selected != null) {
            setCurrentKey(selected);
        } else {
            setCurrentKey(this.currentKey);
        }
    }

    private void initializeView() {
        final ChangeListener tokenStyleChangeListener = e -> {
            final var tokenStyle = getTokenStyleFromView();
            this.parentController.settingsTheme.tokenStyles.put(this.currentKey, tokenStyle);
            TokenSettingKey
                .getTokenTypesForSetting(this.currentKey)
                .forEach(key -> this.textArea
                    .setTokenStyle(key, tokenStyle)
                );
        };
        List.of(
            view.useForeground,
            view.foregroundColorButton,
            view.useBackground,
            view.backgroundColorButton,
            view.isBold,
            view.isItalic,
            view.isUnderline
        ).forEach(component -> component.addChangeListener(tokenStyleChangeListener));

        // selection listener for the token types list
        view.tokenTypesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                final var key = view.tokenTypesList.getSelectedValue();
                if (key != null) {
                    setCurrentKey(key);
                }
            }
        });
    }

    private @NotNull TokenStyle getTokenStyleFromView() {
        final var foreground = this.view.useForeground.isSelected() ? this.view.foregroundColorButton.getColor() : null;
        final var background = this.view.useBackground.isSelected() ? this.view.backgroundColorButton.getColor() : null;
        final var isBold = this.view.isBold.isSelected();
        final var isItalic = this.view.isItalic.isSelected();
        final var isUnderline = this.view.isUnderline.isSelected();
        return new TokenStyle(foreground, background, isBold, isItalic, isUnderline);
    }

    public void setCurrentKey(final @NotNull TokenSettingKey key) {
        this.currentKey = key;
        this.setView(key);
    }

    public void resetButtonValues() {
        this.setView(this.currentKey);
    }

    private void setView(final @NotNull TokenSettingKey key) {
        final var settingsTheme = this.parentController.settingsTheme;
        final var tokenStyle = settingsTheme.tokenStyles.get(key);
        final var foreground = tokenStyle.foreground();
        if (foreground != null) {
            this.view.useForeground.setSelected(true);
            this.view.foregroundColorButton.setColor(foreground);
        } else {
            this.view.useForeground.setSelected(false);
            this.view.foregroundColorButton.setColor(settingsTheme.foregroundColor);
        }
        final var background = tokenStyle.background();
        if (background != null) {
            this.view.useBackground.setSelected(true);
            this.view.backgroundColorButton.setColor(background);
        } else {
            this.view.useBackground.setSelected(false);
            this.view.backgroundColorButton.setColor(settingsTheme.backgroundColor);
        }
        this.view.isBold.setSelected(tokenStyle.isBold());
        this.view.isItalic.setSelected(tokenStyle.isItalic());
        this.view.isUnderline.setSelected(tokenStyle.isUnderline());
    }
}
