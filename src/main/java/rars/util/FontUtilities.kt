package rars.util

import java.awt.GraphicsEnvironment

/**
 * Specialized Font class designed to be used by both the
 * settings menu methods and the Settings class.
 *
 * @author Pete Sanderson
 * @version July 2007
 */
object FontUtilities {
    /**
     * An array of all available font family names. These are guaranteed to
     * be available at runtime, as they come from the local GraphicsEnvironment.
     */
    val allFontFamilies: Array<String> = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .availableFontFamilyNames

    /**
     * Handy utility to produce a string that has all the tabs replaced with spaces
     * characters
     * in the given string. The number of spaces generated is based on the position
     * of
     * the tab character and the editor's current tab size setting.
     *
     * @param this@substituteSpacesForTabs
     * The original string
     * @return New string in which spaces are substituted for tabs
     */
    @JvmStatic
    fun String.substituteSpacesForTabs(tabSize: Int): String = replace("\t", " ".repeat(tabSize))
}
