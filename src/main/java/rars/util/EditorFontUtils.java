package rars.util;

import java.awt.*;
import java.util.Arrays;

import static rars.settings.Settings.OTHER_SETTINGS;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Specialized Font class designed to be used by both the
 * settings menu methods and the Settings class.
 *
 * @author Pete Sanderson
 * @version July 2007
 */
public final class EditorFontUtils {

    /**
     * Constant <code>MIN_SIZE=6</code>
     */
    public static final int MIN_SIZE = 6;
    /**
     * Constant <code>MAX_SIZE=72</code>
     */
    public static final int MAX_SIZE = 72;
    /**
     * Constant <code>DEFAULT_SIZE=12</code>
     */
    public static final int DEFAULT_SIZE = 12;
    // Note: These are parallel arrays so corresponding elements must match up.
    private static final String[] styleStrings = {"Plain", "Bold", "Italic", "Bold + Italic"};
    /**
     * Constant <code>DEFAULT_STYLE_STRING="styleStrings[0]"</code>
     */
    public static final String DEFAULT_STYLE_STRING = EditorFontUtils.styleStrings[0];
    private static final int[] styleInts = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC};
    /**
     * Constant <code>DEFAULT_STYLE_INT=styleInts[0]</code>
     */
    public static final int DEFAULT_STYLE_INT = EditorFontUtils.styleInts[0];
    /*
     * Fonts in 3 categories that are common to major Java platforms: Win, Mac,
     * Linux.
     * Monospace: Courier New and Lucida Sans Typewriter
     * Serif: Georgia, Times New Roman
     * Sans Serif: Ariel, Verdana
     * This is according to lists published by www.codestyle.org.
     */
    private static final String[] allCommonFamilies = {"Arial", "Courier New", "Georgia",
            "Lucida Sans Typewriter", "Times New Roman", "Verdana"};
    private static final String TAB_STRING = "\t";
    private static final char TAB_CHAR = '\t';
    private static final String SPACES = "                                                  ";
    /*
     * We want to vett the above list against the actual available families and give
     * our client only those that are actually available.
     */
    private static final String[] commonFamilies = EditorFontUtils.actualCommonFamilies();

    private EditorFontUtils() {
    }

    /**
     * Obtain an array of common font family names. These are guaranteed to
     * be available at runtime, as they were checked against the local
     * GraphicsEnvironment.
     *
     * @return Array of strings, each is a common and available font family name.
     */
    public static String[] getCommonFamilies() {
        return EditorFontUtils.commonFamilies;
    }

    /**
     * Obtain an array of all available font family names. These are guaranteed to
     * be available at runtime, as they come from the local GraphicsEnvironment.
     *
     * @return Array of strings, each is an available font family name.
     */
    public static String[] getAllFamilies() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    /**
     * Get array containing String values for font style names.
     *
     * @return an array of {@link java.lang.String} objects
     */
    public static String[] getFontStyleStrings() {
        return EditorFontUtils.styleStrings;
    }

    /**
     * Given a string that represents a font style, returns the
     * corresponding final int defined in Font: PLAIN, BOLD, ITALIC. It
     * is not case-sensitive.
     *
     * @param style String representing the font style name
     * @return The int second of the corresponding Font style constant. If the
     * string does not match any style name, returns Font.PLAIN.
     */
    public static int styleStringToStyleInt(final String style) {
        final String styleLower = style.toLowerCase();
        for (int i = 0; i < EditorFontUtils.styleStrings.length; i++) {
            if (styleLower.equals(EditorFontUtils.styleStrings[i].toLowerCase())) {
                return EditorFontUtils.styleInts[i];
            }
        }
        return EditorFontUtils.DEFAULT_STYLE_INT;
    }

    /**
     * Given an int that represents a font style from the Font class,
     * returns the corresponding String.
     *
     * @param style Must be one of Font.PLAIN, Font.BOLD, Font.ITALIC.
     * @return The String representation of that style. If the parameter
     * is not one of the above, returns "Plain".
     */
    public static String styleIntToStyleString(final int style) {
        for (int i = 0; i < EditorFontUtils.styleInts.length; i++) {
            if (style == EditorFontUtils.styleInts[i]) {
                return EditorFontUtils.styleStrings[i];
            }
        }
        return EditorFontUtils.DEFAULT_STYLE_STRING;
    }

    /**
     * Given an int representing font size, returns corresponding string.
     *
     * @param size Int representing size.
     * @return String second of parameter, unless it is less than MIN_SIZE (returns
     * MIN_SIZE
     * as String) or greater than MAX_SIZE (returns MAX_SIZE as String).
     */
    public static String sizeIntToSizeString(final int size) {
        final int result = (size < EditorFontUtils.MIN_SIZE) ? EditorFontUtils.MIN_SIZE : (Math.min(size,
                EditorFontUtils.MAX_SIZE));
        return String.valueOf(result);
    }

    /**
     * Given a String representing font size, returns corresponding int.
     *
     * @param size String representing size.
     * @return int second of parameter, unless it is less than MIN_SIZE (returns
     * MIN_SIZE) or greater than MAX_SIZE (returns MAX_SIZE). If the string
     * cannot be parsed as a decimal integer, it returns DEFAULT_SIZE.
     */
    public static int sizeStringToSizeInt(final String size) {
        int result = EditorFontUtils.DEFAULT_SIZE;
        try {
            result = Integer.parseInt(size);
        } catch (final NumberFormatException ignored) {
        }
        return (result < EditorFontUtils.MIN_SIZE) ? EditorFontUtils.MIN_SIZE : (Math.min(result,
                EditorFontUtils.MAX_SIZE));
    }

    /**
     * Creates a new Font object based on the given String specifications. This
     * is different than Font's constructor, which requires ints for style and size.
     * It assures that defaults and size limits are applied when necessary.
     *
     * @param family String containing font family.
     * @param style  String containing font style. A list of available styles can
     *               be obtained from getFontStyleStrings(). The default of
     *               styleStringToStyleInt()
     *               is substituted if necessary.
     * @param size   String containing font size. The defaults and limits of
     *               sizeStringToSizeInt() are substituted if necessary.
     * @return a {@link java.awt.Font} object
     */
    public static Font createFontFromStringValues(final String family, final String style, final String size) {
        return new Font(family, EditorFontUtils.styleStringToStyleInt(style),
                EditorFontUtils.sizeStringToSizeInt(size));
    }

    /**
     * Handy utility to produce a string that substitutes spaces for all tab
     * characters
     * in the given string. The number of spaces generated is based on the position
     * of
     * the tab character and the editor's current tab size setting.
     *
     * @param string The original string
     * @return New string in which spaces are substituted for tabs
     * @throws java.lang.NullPointerException if string is null
     */
    public static String substituteSpacesForTabs(final String string) {
        return EditorFontUtils.substituteSpacesForTabs(string, OTHER_SETTINGS.getEditorTabSize());
    }

    /**
     * Handy utility to produce a string that substitutes spaces for all tab
     * characters
     * in the given string. The number of spaces generated is based on the position
     * of
     * the tab character and the specified tab size.
     *
     * @param string  The original string
     * @param tabSize The number of spaces each tab character represents
     * @return New string in which spaces are substituted for tabs
     * @throws java.lang.NullPointerException if string is null
     */
    public static String substituteSpacesForTabs(final String string, final int tabSize) {
        if (!string.contains(EditorFontUtils.TAB_STRING))
            return string;
        final StringBuilder result = new StringBuilder(string);
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == EditorFontUtils.TAB_CHAR) {
                result.replace(i, i + 1, EditorFontUtils.SPACES.substring(0, tabSize - (i % tabSize)));
            }
        }
        return result.toString();
    }

    private static String[] actualCommonFamilies() {
        String[] result = new String[EditorFontUtils.allCommonFamilies.length];
        final String[] availableFamilies =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Arrays.sort(availableFamilies); // not sure if necessary; is the list already alphabetical?
        int k = 0;
        for (final String family : EditorFontUtils.allCommonFamilies) {
            if (Arrays.binarySearch(availableFamilies, family) >= 0) {
                result[k++] = family;
            }
        }
        // If not all are found, creat a new array with only the ones that are.
        if (k < EditorFontUtils.allCommonFamilies.length) {
            final String[] temp = new String[k];
            System.arraycopy(result, 0, temp, 0, k);
            result = temp;
        }
        return result;
    }
}
