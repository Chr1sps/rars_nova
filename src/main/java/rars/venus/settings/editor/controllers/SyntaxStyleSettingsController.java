package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;
import rars.venus.editors.TokenStyle;
import rars.venus.settings.editor.views.SyntaxStyleView;

import java.util.HashMap;

import static rars.settings.EditorThemeSettings.EDITOR_THEME_SETTINGS;

public final class SyntaxStyleSettingsController {
    private final @NotNull SyntaxStyleView view;
    private final @NotNull HashMap<@NotNull TokenSettingKey, @NotNull TokenStyle> stylesMap;

    public SyntaxStyleSettingsController(final @NotNull SyntaxStyleView view) {
        this.view = view;
        this.stylesMap = new HashMap<>();
    }

    private void loadValuesFromSettings() {
        this.stylesMap.putAll(EDITOR_THEME_SETTINGS.currentTheme.tokenStyles);
    }
}
