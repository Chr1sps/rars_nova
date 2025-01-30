package rars.settings

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.util.ListenerDispatcher
import rars.venus.editors.TokenStyle
import java.awt.Color
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

interface HighlightingSettings {
    val textSegmentHighlightingStyle: TokenStyle
    val registerHighlightingStyle: TokenStyle?
    val dataSegmentHighlightingStyle: TokenStyle?
}

class HighlightingSettingsImpl(private val preferences: Preferences) : HighlightingSettings {
    private val onChangeDispatcher = ListenerDispatcher<Void?>()
    val onChangeListenerHook = this.onChangeDispatcher.hook

    override var textSegmentHighlightingStyle: TokenStyle = this.preferences.getTokenStyle(
        TEXT_SEGMENT,
        DEFAULT_TEXT_SEGMENT_STYLE,
        HIGHLIGHTING_PREFIX,
        LOGGER
    )
        private set

    override var dataSegmentHighlightingStyle: TokenStyle? = loadNullableTokenStyleFromPreferences(
        DATA_SEGMENT,
        DEFAULT_DATA_SEGMENT_STYLE
    )
        private set

    override var registerHighlightingStyle: TokenStyle? = loadNullableTokenStyleFromPreferences(
        REGISTER,
        DEFAULT_REGISTER_STYLE
    )
        private set

    fun saveSettings() {
        this.preferences.putTokenStyle(
            TEXT_SEGMENT,
            this.textSegmentHighlightingStyle,
            HIGHLIGHTING_PREFIX
        )
        writeNullableTokenStyleToPreferences(DATA_SEGMENT, this.dataSegmentHighlightingStyle)
        writeNullableTokenStyleToPreferences(REGISTER, this.registerHighlightingStyle)
        try {
            this.preferences.flush()
        } catch (_: SecurityException) {
            LOGGER.error("Unable to write to persistent storage for security reasons.")
        } catch (_: BackingStoreException) {
            LOGGER.error("Unable to communicate with persistent storage.")
        }
        this.onChangeDispatcher.dispatch(null)
    }

    private fun writeNullableTokenStyleToPreferences(
        type: String,
        style: TokenStyle?
    ) {
        if (style == null) {
            this.preferences.putBoolean(enabledPrefix(type), false)
        } else {
            this.preferences.putBoolean(enabledPrefix(type), true)
            this.preferences.putTokenStyle(type, style, HIGHLIGHTING_PREFIX)
        }
    }

    private fun loadNullableTokenStyleFromPreferences(
        type: String,
        defaultStyle: TokenStyle
    ): TokenStyle? {
        val enabled = this.preferences.getBoolean(enabledPrefix(type), true)
        return if (!enabled) {
            null
        } else
            this.preferences.getTokenStyle(
                type,
                defaultStyle,
                HIGHLIGHTING_PREFIX,
                LOGGER
            )
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(HighlightingSettingsImpl::class.java)

        // region Preferences keys
        private const val HIGHLIGHTING_PREFIX = "Highlighting"

        private const val ENABLED = "Enabled"

        private const val TEXT_SEGMENT = "Text"
        private const val DATA_SEGMENT = "Data"
        private const val REGISTER = "Register"
        // endregion Preferences keys

        private fun enabledPrefix(type: String) = "$HIGHLIGHTING_PREFIX$type$ENABLED"
    }
}

@JvmField
val DEFAULT_TEXT_SEGMENT_STYLE: TokenStyle = fromBackground(Color(0xFFFF99))

@JvmField
val DEFAULT_DATA_SEGMENT_STYLE: TokenStyle = fromBackground(Color(0x99CCFF))

@JvmField
val DEFAULT_REGISTER_STYLE: TokenStyle = fromBackground(Color(0x99CC55))

private fun fromBackground(color: Color): TokenStyle = TokenStyle(
    Color.BLACK,
    color,
    false,
    false,
    false
)
