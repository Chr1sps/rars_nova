package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.SettingsThemePresets;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.PresetsView;

public final class PresetsController {
    private final @NotNull PresetsView view;

    public PresetsController(
        final @NotNull PresetsView view,
        final @NotNull TextEditingArea textEditingArea) {
        this.view = view;
        SettingsThemePresets.THEMES.forEach(themeEntry -> {
            final var themeName = themeEntry.name();
            final var settingsTheme = themeEntry.theme();
            final var section = new PresetsView.PresetSection(themeName, settingsTheme);
            section.button.addActionListener(e -> {
                final var theme = settingsTheme.toTheme();
                textEditingArea.setTheme(theme);
                // TODO: sync with base style and syntax style settings
            });
            view.addSection(section);
        });
    }
}
