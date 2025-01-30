package rars.settings

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.util.ListenerDispatcher
import rars.venus.editors.TokenStyle
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

interface EditorThemeSettings {
    val currentTheme: SettingsTheme
}

class EditorThemeSettingsImpl(private val preferences: Preferences) : EditorThemeSettings {
    private val onChangeDispatcher = ListenerDispatcher<Void?>()

    @JvmField
    val onChangeListenerHook = onChangeDispatcher.hook

    /**
     * The current theme in memory. You can make changes to this theme and then
     * call [saveSettingsToPreferences] to save the changes to the preferences.
     */
    private var _currentTheme: SettingsTheme = loadThemeFromPreferences()
    private var backupTheme: SettingsTheme = _currentTheme.clone()

    override var currentTheme: SettingsTheme
        get() = _currentTheme.clone()
        set(value) {
            this._currentTheme = value
        }

    /**
     * Commits the theme currently in memory to the preferences. If the commit
     * fails, the theme in memory will be reverted to the previous state.
     */
    fun saveSettingsToPreferences() {
        writeThemeToPreferences(this._currentTheme)
        try {
            this.preferences.flush()
            this.backupTheme = this._currentTheme
            this.onChangeDispatcher.dispatch(null)
        } catch (_: SecurityException) {
            LOGGER.error("Unable to write to persistent storage for security reasons. Reverting to previous settings.")
            // The reason why we need to write the backup theme to the preferences
            // is because the Preferences API implementations are free to flush
            // the changes to disk at any time.
            writeThemeToPreferences(this.backupTheme)
            this._currentTheme = this.backupTheme
        } catch (_: BackingStoreException) {
            LOGGER.error("Unable to communicate with persistent storage. Reverting to previous settings.")
            writeThemeToPreferences(this.backupTheme)
            this._currentTheme = this.backupTheme
        }
    }

    private fun writeThemeToPreferences(settingsTheme: SettingsTheme) {
        this.preferences.putColor(THEME_PREFIX + BACKGROUND, settingsTheme.backgroundColor)
        this.preferences.putColor(THEME_PREFIX + FOREGROUND, settingsTheme.foregroundColor)
        this.preferences.putColor(THEME_PREFIX + LINE_HIGHLIGHT, settingsTheme.lineHighlightColor)
        this.preferences.putColor(THEME_PREFIX + CARET, settingsTheme.caretColor)
        this.preferences.putColor(THEME_PREFIX + SELECTION, settingsTheme.selectionColor)
        settingsTheme.tokenStyles.forEach { (key, style) ->
            this.preferences.putTokenStyle(
                key.ordinal.toString(),
                style,
                THEME_PREFIX + STYLES
            )
        }
    }

    // region Preference loading methods
    private fun loadThemeFromPreferences(): SettingsTheme {
        val defaultTheme = SettingsTheme.DEFAULT_THEME
        val background = preferences.getColor(THEME_PREFIX + BACKGROUND, defaultTheme.backgroundColor, LOGGER)
        val foreground = preferences.getColor(THEME_PREFIX + FOREGROUND, defaultTheme.foregroundColor, LOGGER)
        val lineHighlight = preferences.getColor(THEME_PREFIX + LINE_HIGHLIGHT, defaultTheme.lineHighlightColor, LOGGER)
        val caret = preferences.getColor(THEME_PREFIX + CARET, defaultTheme.caretColor, LOGGER)
        val selection = preferences.getColor(THEME_PREFIX + SELECTION, defaultTheme.selectionColor, LOGGER)
        val tokenStyles = loadTokenStylesFromPreferences(defaultTheme.tokenStyles)
        return SettingsTheme(background, foreground, lineHighlight, caret, selection, tokenStyles)
    }

    private fun loadTokenStylesFromPreferences(
        defaultColorScheme: MutableMap<TokenSettingKey, TokenStyle>
    ): MutableMap<TokenSettingKey, TokenStyle> {
        val styleMap = HashMap<TokenSettingKey, TokenStyle>()
        for (type in TokenSettingKey.entries) {
            styleMap.put(
                type, this.preferences.getTokenStyle(
                    type.ordinal.toString(),
                    defaultColorScheme[type]!!,
                    THEME_PREFIX + STYLES,
                    LOGGER
                )
            )
        }
        return styleMap
    }
    // endregion Preference loading methods

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(EditorThemeSettingsImpl::class.java)

        /**
         * Top level theme settings prefix.
         */
        private const val THEME_PREFIX = "Theme"

        // region Preferences keys
        private const val BACKGROUND = "Background"
        private const val FOREGROUND = "Foreground"
        private const val LINE_HIGHLIGHT = "LineHighlight"
        private const val CARET = "Caret"
        private const val SELECTION = "Selection"
        private const val STYLES = "Styles"
        private const val BOLD = "Bold"
        private const val ITALIC = "Italic"
        private const val UNDERLINE = "Underline"
        // endregion Preferences keys

        // region Preferences prefix methods
        private fun foregroundPrefix(key: TokenSettingKey): String = "$THEME_PREFIX$STYLES${key.ordinal}$FOREGROUND"
        private fun backgroundPrefix(key: TokenSettingKey): String = "$THEME_PREFIX$STYLES${key.ordinal}$BACKGROUND"
        private fun boldPrefix(key: TokenSettingKey): String = "$THEME_PREFIX$STYLES${key.ordinal}$BOLD"
        private fun italicPrefix(key: TokenSettingKey): String = "$THEME_PREFIX$STYLES${key.ordinal}$ITALIC"
        private fun underlinePrefix(key: TokenSettingKey): String = "$THEME_PREFIX$STYLES${key.ordinal}$UNDERLINE"
        // endregion Preferences prefix methods
    }
}