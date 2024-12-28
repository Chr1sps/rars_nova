package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.SettingsThemePresets;
import rars.venus.editors.TextEditingArea;
import rars.venus.settings.editor.views.PresetsView;

public final class PresetsController {
    public PresetsController(
        final @NotNull PresetsView view,
        final @NotNull TextEditingArea textArea,
        final @NotNull EditorSettingsController parentController
    ) {
        SettingsThemePresets.THEMES.forEach(themeEntry -> {
            final var section = new PresetsView.PresetSection(themeEntry.name(), themeEntry.theme());
            section.button.addActionListener(e -> {
                parentController.settingsTheme = themeEntry.theme().clone();
                textArea.setTheme(parentController.settingsTheme.toEditorTheme());
                // We need to update the info regarding the theme in all the theme
                // related controllers.
                parentController.updateThemeControllers();
            });
            view.addSection(section);
        });
    }
}
