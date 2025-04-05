package rars.settings

import org.apache.logging.log4j.Logger
import rars.util.toHexString
import rars.venus.editors.TokenStyle
import java.awt.Color
import java.util.prefs.Preferences

// region Preferences keys
private const val BACKGROUND = "Background"
private const val FOREGROUND = "Foreground"
private const val BOLD = "Bold"
private const val ITALIC = "Italic"
private const val UNDERLINE = "Underline"
// endregion Preferences keys

// region Writing methods
internal fun Preferences.putTokenStyle(
    key: String,
    style: TokenStyle,
    commonPrefix: String
) {
    this.putNullableColor("$commonPrefix$key$FOREGROUND", style.foreground)
    this.putNullableColor("$commonPrefix$key$BACKGROUND", style.background)
    this.putBoolean("$commonPrefix$key$BOLD", style.isBold)
    this.putBoolean("$commonPrefix$key$ITALIC", style.isItalic)
    this.putBoolean("$commonPrefix$key$UNDERLINE", style.isUnderline)
}

/**
 * If the value is null, save "null" to the preferences.
 * Otherwise, save the value to the preferences.
 */
internal fun Preferences.putNullableColor(key: String, value: Color?) {
    val valueString: String = value?.toHexString() ?: "null"
    this.put(key, valueString)
}

internal fun Preferences.putColor(key: String, value: Color) {
    this.put(key, value.toHexString())
}

// endregion Writing methods

// region Loading methods

internal fun Preferences.getTokenStyle(
    key: String,
    defaultStyle: TokenStyle,
    commonPrefix: String,
    logger: Logger
): TokenStyle {
    val foreground = this.getNullableColor("$commonPrefix$key$FOREGROUND", defaultStyle.foreground, logger)
    val background = this.getNullableColor("$commonPrefix$key$BACKGROUND", defaultStyle.background, logger)
    val isBold = this.getBoolean("$commonPrefix$key$BOLD", defaultStyle.isBold)
    val isItalic = this.getBoolean("$commonPrefix$key$ITALIC", defaultStyle.isItalic)
    val isUnderline = this.getBoolean("$commonPrefix$key$UNDERLINE", defaultStyle.isUnderline)
    return TokenStyle(foreground, background, isBold, isItalic, isUnderline)
}

internal fun Preferences.getColor(
    key: String, defaultValue: Color, logger: Logger
): Color {
    val value = this.get(key, null)
    return if (value == null) {
        defaultValue
    } else try {
        Color.decode(value)
    } catch (nfe: NumberFormatException) {
        logger.error("Unable to decode color from preferences", nfe)
        defaultValue
    }
}

internal fun Preferences.getNullableColor(
    key: String,
    defaultValue: Color?,
    logger: Logger
): Color? {
    val value = this.get(key, null)
    if (value == null) {
        return defaultValue
    } else if (value == "null") {
        return null
    }
    try {
        return Color.decode(value)
    } catch (nfe: NumberFormatException) {
        logger.error("Unable to decode color from preferences", nfe)
        return defaultValue
    }
}

// endregion Loading methods