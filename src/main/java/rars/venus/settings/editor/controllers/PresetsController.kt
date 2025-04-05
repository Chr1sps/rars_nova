package rars.venus.settings.editor.controllers

import rars.settings.SettingsThemePresets.THEMES
import rars.venus.editors.TextEditingArea
import rars.venus.settings.editor.views.PresetsView
import rars.venus.settings.editor.views.PresetsView.PresetSection

fun PresetsController(
    view: PresetsView,
    textArea: TextEditingArea,
    parentController: EditorSettingsController
) {
    THEMES.forEach { themeEntry ->
        val section = PresetSection(themeEntry.name)
        section.button.addActionListener {
            parentController.settingsTheme = themeEntry.theme.clone()
            textArea.setTheme(parentController.settingsTheme.toEditorTheme())
            // We need to update the info regarding the theme in all the theme
            // related controllers.
            parentController.updateThemeControllers()
        }
        view.addSection(section)
    }
}
