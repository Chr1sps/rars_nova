package rars.settings

data class AllSettings(
    @JvmField val boolSettings: BoolSettingsImpl,
    @JvmField val fontSettings: FontSettingsImpl,
    @JvmField val editorThemeSettings: EditorThemeSettingsImpl,
    @JvmField val highlightingSettings: HighlightingSettingsImpl,
    @JvmField val otherSettings: OtherSettingsImpl,
) 
