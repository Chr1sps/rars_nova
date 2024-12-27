package rars.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public final class FontUtilities {

    public static final int MIN_SIZE = 6;
    public static final int MAX_SIZE = 72;
    public static final int DEFAULT_SIZE = 12;
    /**
     * An array of all available font family names. These are guaranteed to
     * be available at runtime, as they come from the local GraphicsEnvironment.
     */
    public static String[] allFontFamilies = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .getAvailableFontFamilyNames();

    private FontUtilities() {
    }

    /**
     * Handy utility to produce a string that has all the tabs replaced with spaces
     * characters
     * in the given string. The number of spaces generated is based on the position
     * of
     * the tab character and the editor's current tab size setting.
     *
     * @param string The original string
     * @return New string in which spaces are substituted for tabs
     */
    public static @NotNull String substituteSpacesForTabs(final @NotNull String string) {
        final var tabSize = OTHER_SETTINGS.getEditorTabSize();
        return IntStream.range(0, string.length())
            .mapToObj(i -> {
                if (string.charAt(i) == '\t') {
                    return " ".repeat(tabSize - (i % tabSize));
                } else {
                    return String.valueOf(string.charAt(i));
                }
            })
            .collect(Collectors.joining());
    }
}
