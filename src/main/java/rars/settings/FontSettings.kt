package rars.settings

import rars.logging.RARSLogging
import rars.logging.error
import rars.util.FontWeight
import rars.util.ListenerDispatcher
import java.awt.Font
import java.awt.font.TextAttribute
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

interface FontSettings {
    val fontWeight: FontWeight
    val isLigaturized: Boolean
    val currentFont: Font
    val fontFamily: String
    val fontSize: Int
}

interface PreferencesSettings {
    fun saveSettingsToPreferences()
}

interface MutableFontSettings : FontSettings {
    override var fontWeight: FontWeight
    override var isLigaturized: Boolean
    override var currentFont: Font
    override var fontFamily: String
    override var fontSize: Int
}

class FontSettingsImpl(private val preferences: Preferences) : FontSettings {
    private val onChangeDispatcher = ListenerDispatcher<Unit>()

    @JvmField
    val onChangeListenerHook = this.onChangeDispatcher.hook

    override var fontSize: Int =
        preferences.getInt(FONT_PREFIX + SIZE, DEFAULT_FONT_SIZE)
        set(value) {
            if (value < MIN_FONT_SIZE || value > MAX_FONT_SIZE) {
                LOGGER.error { "Attempted to set invalid font size: $value" }
                error("Font size must be between$MIN_FONT_SIZE and $MAX_FONT_SIZE. Provided: $value")
            }
            field = value
        }

    override var fontFamily: String = preferences.get(
        FONT_PREFIX + FAMILY,
        DEFAULT_FONT_FAMILY
    )
    override var fontWeight: FontWeight = FontWeight.valueOf(
        preferences.get(
            FONT_PREFIX + WEIGHT,
            DEFAULT_FONT_WEIGHT.name
        )
    )
    override var isLigaturized: Boolean = preferences.getBoolean(
        FONT_PREFIX + LIGATURES,
        DEFAULT_LIGATURES
    )
    override val currentFont: Font
        get() {
            val attributes = buildMap {
                put(TextAttribute.FAMILY, fontFamily)
                put(TextAttribute.SIZE, fontSize)
                put(TextAttribute.WEIGHT, fontWeight.weight)
                if (isLigaturized) {
                    put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON)
                }
            }
            return Font(attributes)
        }

    fun saveSettingsToPreferences() {
        preferences.put(FONT_PREFIX + FAMILY, fontFamily)
        preferences.putInt(FONT_PREFIX + SIZE, fontSize)
        preferences.putBoolean(FONT_PREFIX + LIGATURES, isLigaturized)
        try {
            this.preferences.flush()
        } catch (_: SecurityException) {
            LOGGER.error { "Unable to write to persistent storage for security reasons." }
        } catch (_: BackingStoreException) {
            LOGGER.error { "Unable to communicate with persistent storage." }
        }
        this.onChangeDispatcher.dispatch(Unit)
    }

    companion object {
        private val LOGGER = RARSLogging.forClass(FontSettingsImpl::class)

        private const val MIN_FONT_SIZE = 6
        private const val MAX_FONT_SIZE = 72

        // region Defaults
        private const val DEFAULT_FONT_FAMILY = "Monospaced"
        private const val DEFAULT_FONT_SIZE = 14
        private val DEFAULT_FONT_WEIGHT = FontWeight.REGULAR
        private const val DEFAULT_LIGATURES = false
        // endregion Defaults

        // region Preference keys
        private const val FONT_PREFIX = "Font"
        private const val SIZE = "Size"
        private const val FAMILY = "Family"
        private const val WEIGHT = "Weight"
        private const val LIGATURES = "Ligatures"
        // endregion Preference keys
    }
}