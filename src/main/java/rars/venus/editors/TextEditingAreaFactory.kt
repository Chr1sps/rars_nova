package rars.venus.editors

import rars.settings.AllSettings
import rars.venus.editors.rsyntaxtextarea.RSyntaxTextAreaBasedEditor

fun createTextEditingArea(allSettings: AllSettings): TextEditingArea = RSyntaxTextAreaBasedEditor(
    allSettings.editorThemeSettings.currentTheme.toEditorTheme(),
    allSettings.fontSettings
)
