package io.github.chr1sps.rars;

import io.github.chr1sps.rars.notices.SettingsNotice;
import io.github.chr1sps.rars.util.Binary;
import io.github.chr1sps.rars.util.EditorFont;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.SubmissionPublisher;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Contains various IDE settings. Persistent settings are maintained for the
 * current user and on the current machine using
 * Java's Preference objects. Failing that, default setting values come from
 * Settings.properties file. If both of those fail, default values come from
 * static arrays defined in this class. The latter can can be modified prior to
 * instantiating Settings object.
 * <p>
 * NOTE: If the Preference objects fail due to security exceptions, changes to
 * settings will not carry over from one RARS session to the next.
 * <p>
 * Actual implementation of the Preference objects is platform-dependent.
 * For Windows, they are stored in Registry. To see, run regedit and browse to:
 * HKEY_CURRENT_USER\Software\JavaSoft\Prefs\rars
 *
 * @author Pete Sanderson
 */
public class Settings extends SubmissionPublisher<SettingsNotice> {
    /**
     * Current specified exception handler file (a RISCV assembly source file)
     */
    public static final int EXCEPTION_HANDLER = 0;
    /**
     * Order of text segment table columns
     */
    public static final int TEXT_COLUMN_ORDER = 1;
    /**
     * State for sorting label window display
     */
    public static final int LABEL_SORT_STATE = 2;
    /**
     * Identifier of current memory configuration
     */
    public static final int MEMORY_CONFIGURATION = 3;

    // region String settings
    // STRING SETTINGS. Each array position has associated name.
    /**
     * Caret blink rate in milliseconds, 0 means don't blink.
     */
    public static final int CARET_BLINK_RATE = 4;
    /**
     * Editor tab size in characters.
     */
    public static final int EDITOR_TAB_SIZE = 5;
    /**
     * Number of letters to be matched by editor's instruction guide before popup
     * generated (if popup enabled)
     */
    public static final int EDITOR_POPUP_PREFIX_LENGTH = 6;
    /**
     * Font for the text editor
     */
    public static final int EDITOR_FONT = 0;
    /**
     * Font for table even row background (text, data, register displays)
     */
    public static final int EVEN_ROW_FONT = 1;
    /**
     * Font for table odd row background (text, data, register displays)
     */
    public static final int ODD_ROW_FONT = 2;
    /**
     * Font for table odd row foreground (text, data, register displays)
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_FONT = 3;
    /**
     * Font for text segment delay slot highlighted background
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT = 4;
    /**
     * Font for text segment highlighted background
     */
    public static final int DATASEGMENT_HIGHLIGHT_FONT = 5;

    // FONT SETTINGS. Each array position has associated name.
    /**
     * Font for register highlighted background
     */
    public static final int REGISTER_HIGHLIGHT_FONT = 6;
    /**
     * RGB color for table even row background (text, data, register displays)
     */
    public static final int EVEN_ROW_BACKGROUND = 0;
    /**
     * RGB color for table even row foreground (text, data, register displays)
     */
    public static final int EVEN_ROW_FOREGROUND = 1;
    /**
     * RGB color for table odd row background (text, data, register displays)
     */
    public static final int ODD_ROW_BACKGROUND = 2;
    /**
     * RGB color for table odd row foreground (text, data, register displays)
     */
    public static final int ODD_ROW_FOREGROUND = 3;
    /**
     * RGB color for text segment highlighted background
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_BACKGROUND = 4;
    /**
     * RGB color for text segment highlighted foreground
     */
    public static final int TEXTSEGMENT_HIGHLIGHT_FOREGROUND = 5;
    /**
     * RGB color for text segment delay slot highlighted background
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND = 6;
    /**
     * RGB color for text segment delay slot highlighted foreground
     */
    public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND = 7;
    /**
     * RGB color for text segment highlighted background
     */
    public static final int DATASEGMENT_HIGHLIGHT_BACKGROUND = 8;
    /**
     * RGB color for text segment highlighted foreground
     */
    public static final int DATASEGMENT_HIGHLIGHT_FOREGROUND = 9;
    /**
     * RGB color for register highlighted background
     */
    public static final int REGISTER_HIGHLIGHT_BACKGROUND = 10;
    /**
     * RGB color for register highlighted foreground
     */
    public static final int REGISTER_HIGHLIGHT_FOREGROUND = 11;

    // COLOR SETTINGS. Each array position has associated name.
    /**
     * RGB background color of Editor
     */
    public static final int EDITOR_BACKGROUND = 12;
    /**
     * RGB foreground color of Editor
     */
    public static final int EDITOR_FOREGROUND = 13;
    /**
     * RGB line-highlight color of Editor
     */
    public static final int EDITOR_LINE_HIGHLIGHT = 14;
    /**
     * RGB color of text-selection in Editor
     */
    public static final int EDITOR_SELECTION_COLOR = 15;
    /**
     * RGB color of caret in Editor
     */
    public static final int EDITOR_CARET_COLOR = 16;
    private static final Logger LOGGER = LogManager.getLogger();
    /* Properties file used to hold default settings. */
    private static final String settingsFile = "Settings";
    // Match the above by position.
    // endregion String settings
    private static final String[] stringSettingsKeys = {"ExceptionHandler", "TextColumnOrder", "LabelSortState",
            "MemoryConfiguration", "CaretBlinkRate", "EditorTabSize", "EditorPopupPrefixLength"};
    /**
     * Last resort default values for String settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match key by list position.
     */
    private static final String[] defaultStringSettingsValues = {"", "0 1 2 3 4", "0", "", "500", "8", "2"};
    private static final String[] fontFamilySettingsKeys = {"EditorFontFamily", "EvenRowFontFamily",
            "OddRowFontFamily", " TextSegmentHighlightFontFamily", "TextSegmentDelayslotHighightFontFamily",
            "DataSegmentHighlightFontFamily", "RegisterHighlightFontFamily"
    };
    private static final String[] fontStyleSettingsKeys = {"EditorFontStyle", "EvenRowFontStyle",
            "OddRowFontStyle", " TextSegmentHighlightFontStyle", "TextSegmentDelayslotHighightFontStyle",
            "DataSegmentHighlightFontStyle", "RegisterHighlightFontStyle"
    };
    private static final String[] fontSizeSettingsKeys = {"EditorFontSize", "EvenRowFontSize",
            "OddRowFontSize", " TextSegmentHighlightFontSize", "TextSegmentDelayslotHighightFontSize",
            "DataSegmentHighlightFontSize", "RegisterHighlightFontSize"
    };
    /**
     * Last resort default values for Font settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match key by list position shown above.
     */

    // DPS 3-Oct-2012
    // Changed default font family from "Courier New" to "Monospaced" after
    // receiving reports that Mac were not
    // correctly rendering the left parenthesis character in the editor or text
    // segment display.
    // See
    // http://www.mirthcorp.com/community/issues/browse/MIRTH-1921?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
    private static final String[] defaultFontFamilySettingsValues = {"Monospaced", "Monospaced", "Monospaced",
            "Monospaced", "Monospaced", "Monospaced", "Monospaced"
    };
    private static final String[] defaultFontStyleSettingsValues = {"Plain", "Plain", "Plain", "Plain",
            "Plain", "Plain", "Plain"
    };
    private static final String[] defaultFontSizeSettingsValues = {"12", "12", "12", "12", "12", "12", "12",
    };
    // Match the above by position.
    private static final String[] colorSettingsKeys = {
            "EvenRowBackground", "EvenRowForeground", "OddRowBackground", "OddRowForeground",
            "TextSegmentHighlightBackground", "TextSegmentHighlightForeground",
            "TextSegmentDelaySlotHighlightBackground", "TextSegmentDelaySlotHighlightForeground",
            "DataSegmentHighlightBackground", "DataSegmentHighlightForeground",
            "RegisterHighlightBackground", "RegisterHighlightForeground",
            "EditorBackground", "EditorForeground", "EditorLineHighlight", "EditorSelection", "EditorCaretColor"};
    /**
     * Last resort default values for color settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match key by list position.
     */
    private static final String[] defaultColorSettingsValues = {
            "0x00e0e0e0", "0", "0x00ffffff", "0", "0x00ffff99", "0", "0x0033ff00", "0", "0x0099ccff", "0", "0x0099cc55",
            "0", "0x00ffffff", "0x00000000", "0x00eeeeee", "0x00ccccff", "0x00000000"};
    private static final String SYNTAX_STYLE_COLOR_PREFIX = "SyntaxStyleColor_";
    private static final String SYNTAX_STYLE_BOLD_PREFIX = "SyntaxStyleBold_";
    private static final String SYNTAX_STYLE_ITALIC_PREFIX = "SyntaxStyleItalic_";
    private static String[] syntaxStyleColorSettingsKeys, syntaxStyleBoldSettingsKeys, syntaxStyleItalicSettingsKeys;
    private static String[] defaultSyntaxStyleColorSettingsValues;
    private static boolean[] defaultSyntaxStyleBoldSettingsValues;
    private static boolean[] defaultSyntaxStyleItalicSettingsValues;
    private final ColorMode defaultColorMode = ColorMode.SYSTEM;
    private final HashMap<Bool, Boolean> booleanSettingsValues;
    private final String[] stringSettingsValues;
    private final String[] fontFamilySettingsValues;
    private final String[] fontStyleSettingsValues;
    private final String[] fontSizeSettingsValues;
    /**
     * Color settings, either a hex-encoded value or a value of
     * {@link ColorMode#modeKey}
     */
    private final String[] colorSettingsValues;
    private final Preferences preferences;
    private SystemColorProvider[] systemColors;
    /*
     * **************************************************************************
     * This section contains all code related to syntax highlighting styles
     * settings.
     * A style includes 3 components: color, bold (t/f), italic (t/f)
     *
     * The fallback defaults will come not from an array here, but from the
     * existing static method SyntaxUtilities.getDefaultSyntaxStyles()
     * in the rars.venus.editors.jeditsyntax package. It returns an array
     * of SyntaxStyle objects.
     *
     */
    private String[] syntaxStyleColorSettingsValues;
    private boolean[] syntaxStyleBoldSettingsValues;
    private boolean[] syntaxStyleItalicSettingsValues;
    /**
     * Create Settings object and set to saved values. If saved values not found,
     * will set
     * based on defaults stored in Settings.properties file. If file problems, will
     * set based
     * on defaults stored in this class.
     */
    public Settings() {
        this.booleanSettingsValues = new HashMap<>();
        this.stringSettingsValues = new String[Settings.stringSettingsKeys.length];
        this.fontFamilySettingsValues = new String[Settings.fontFamilySettingsKeys.length];
        this.fontStyleSettingsValues = new String[Settings.fontStyleSettingsKeys.length];
        this.fontSizeSettingsValues = new String[Settings.fontSizeSettingsKeys.length];
        this.colorSettingsValues = new String[Settings.colorSettingsKeys.length];
        // This determines where the values are actually stored. Actual implementation
        // is platform-dependent. For Windows, they are stored in Registry. To see,
        // run regedit and browse to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\rars
        this.preferences = Preferences.userNodeForPackage(this.getClass());
        // The gui parameter, formerly passed to initialize(), is no longer needed
        // because I removed (1/21/09) the call to generate the Font object for the text
        // editor.
        // Font objects are now generated only on demand so the "if (gui)" guard
        // is no longer necessary. Originally added by Berkeley b/c they were running it
        // on a
        // headless server and running in command mode. The Font constructor resulted in
        // Swing
        // initialization which caused problems. Now this will only occur on demand from
        // Venus, which happens only when running as GUI.
        this.initialize();
    }

    /**
     * Return whether backstepping is permitted at this time. Backstepping is
     * ability to undo execution
     * steps one at a time. Available only in the IDE. This is not a persistent
     * setting and is not under
     * RARS user control.
     *
     * @return true if backstepping is permitted, false otherwise.
     */
    public static boolean getBackSteppingEnabled() {
        return (Globals.program != null && Globals.program.getBackStepper() != null
                && Globals.program.getBackStepper().enabled());
    }

    /**
     * Retrieve a default Font setting
     *
     * @param fontSettingPosition constant that identifies which item
     * @return Font object for given item
     */
    public static Font getDefaultFontByPosition(final int fontSettingPosition) {
        if (fontSettingPosition >= 0 && fontSettingPosition < Settings.defaultFontFamilySettingsValues.length) {
            return EditorFont.createFontFromStringValues(Settings.defaultFontFamilySettingsValues[fontSettingPosition],
                    Settings.defaultFontStyleSettingsValues[fontSettingPosition],
                    Settings.defaultFontSizeSettingsValues[fontSettingPosition]);
        } else {
            return null;
        }
    }

    /**
     * Get the text editor default tab size in characters
     *
     * @return tab size in characters
     */
    public static int getDefaultEditorTabSize() {
        return Integer.parseInt(Settings.defaultStringSettingsValues[Settings.EDITOR_TAB_SIZE]);
    }

    private static Color mixColors(final Color a, final Color b, final float ratio) {
        return new Color(
                ((float) a.getRed()) / 256 * ratio + ((float) b.getRed()) / 256 * (1 - ratio),
                ((float) a.getGreen()) / 256 * ratio + ((float) b.getGreen()) / 256 * (1 - ratio),
                ((float) a.getBlue()) / 256 * ratio + ((float) b.getBlue()) / 256 * (1 - ratio),
                ((float) a.getAlpha()) / 256 * ratio + ((float) b.getAlpha()) / 256 * (1 - ratio));
    }

    private static Color getColorValueByString(final int position, String colorStr, final String[] defaults,
                                               final SystemColorProvider[] system) {
        Color color = null;
        if (colorStr != null) {
            if (colorStr.equalsIgnoreCase(ColorMode.DEFAULT.modeKey)) {
                colorStr = null; // Trigger default
            } else if (system != null && colorStr.equalsIgnoreCase(ColorMode.SYSTEM.modeKey)) {
                color = Settings.getSystemColorByPosition(position, system);
            }
        }
        if (color == null && colorStr == null) {
            colorStr = Settings.getColorStringByPosition(position, defaults);
        }
        if (color == null && colorStr != null) {
            try {
                color = Color.decode(colorStr);
            } catch (final NumberFormatException ignored) {
            }
        }
        return color;
    }

    private static ColorMode getColorModeByPostion(final int position, final String[] values) {
        final String colorStr = Settings.getColorStringByPosition(position, values);
        if (colorStr == null)
            return ColorMode.CUSTOM;

        for (final ColorMode mode : ColorMode.values())
            if (mode.modeKey != null && mode.modeKey.equalsIgnoreCase(colorStr))
                return mode;

        return ColorMode.CUSTOM;
    }

    private static String getColorStringByPosition(final int position, final String[] values) {
        if (values == null)
            return null;
        if (position >= 0 && position < values.length)
            return values[position];
        return null;
    }

    private static Color getSystemColorByPosition(final int position, final SystemColorProvider[] providers) {
        if (position >= 0 && position < providers.length) {
            final SystemColorProvider provider = providers[position];
            if (provider != null)
                return provider.getColor();
        }
        return null;
    }

    /*
     * Private helper to do the work of converting a string containing Text
     * Segment window table column order into int array and returning it.
     * If a problem occurs with the parameter string, will fall back to the
     * default defined above.
     */
    private static int[] getTextSegmentColumnOrder(final String stringOfColumnIndexes) {
        final StringTokenizer st = new StringTokenizer(stringOfColumnIndexes);
        final int[] list = new int[st.countTokens()];
        int index = 0, value;
        boolean valuesOK = true;
        while (st.hasMoreTokens()) {
            try {
                value = Integer.parseInt(st.nextToken());
            } // could be either NumberFormatException or NoSuchElementException
            catch (final Exception e) {
                valuesOK = false;
                break;
            }
            list[index++] = value;
        }
        if (!valuesOK && !stringOfColumnIndexes.equals(Settings.defaultStringSettingsValues[Settings.TEXT_COLUMN_ORDER])) {
            return Settings.getTextSegmentColumnOrder(Settings.defaultStringSettingsValues[Settings.TEXT_COLUMN_ORDER]);
        }
        return list;
    }

    /**
     * Reset settings to default values, as described in the constructor comments.
     */
    public void reset() {
        this.initialize();
    }

    /**
     * <p>setEditorSyntaxStyleByPosition.</p>
     *
     * @param index       a int
     * @param syntaxStyle a {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle} object
     */
    public void setEditorSyntaxStyleByPosition(final int index, final SyntaxStyle syntaxStyle) {
        this.syntaxStyleColorSettingsValues[index] = syntaxStyle.getColorAsHexString();
        this.syntaxStyleItalicSettingsValues[index] = syntaxStyle.isItalic();
        this.syntaxStyleBoldSettingsValues[index] = syntaxStyle.isBold();
        this.saveEditorSyntaxStyle(index);
    }
    // *********************************************************************************

    ////////////////////////////////////////////////////////////////////////
    // Setting Getters
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>getEditorSyntaxStyleByPosition.</p>
     *
     * @param index a int
     * @return a {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle} object
     */
    public SyntaxStyle getEditorSyntaxStyleByPosition(final int index) {
        return new SyntaxStyle(
                this.getColorValueByPosition(index, this.syntaxStyleColorSettingsValues, Settings.defaultSyntaxStyleColorSettingsValues,
                        null),
                this.syntaxStyleItalicSettingsValues[index],
                this.syntaxStyleBoldSettingsValues[index]);
    }

    /**
     * <p>getDefaultEditorSyntaxStyleByPosition.</p>
     *
     * @param index a int
     * @return a {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle} object
     */
    public SyntaxStyle getDefaultEditorSyntaxStyleByPosition(final int index) {
        return new SyntaxStyle(this.getColorValueByPosition(index, Settings.defaultSyntaxStyleColorSettingsValues, null, null),
                Settings.defaultSyntaxStyleItalicSettingsValues[index],
                Settings.defaultSyntaxStyleBoldSettingsValues[index]);
    }

    private void saveEditorSyntaxStyle(final int index) {
        try {
            this.preferences.put(Settings.syntaxStyleColorSettingsKeys[index], this.syntaxStyleColorSettingsValues[index]);
            this.preferences.putBoolean(Settings.syntaxStyleBoldSettingsKeys[index], this.syntaxStyleBoldSettingsValues[index]);
            this.preferences.putBoolean(Settings.syntaxStyleItalicSettingsKeys[index], this.syntaxStyleItalicSettingsValues[index]);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
    }

    // For syntax styles, need to initialize from SyntaxUtilities defaults.
    // Taking care not to explicitly create a Color object, since it may trigger
    // Swing initialization (that caused problems for UC Berkeley when we
    // created Font objects here). It shouldn't, but then again Font shouldn't
    // either but they said it did. (see HeadlessException)
    // On othe other hand, the first statement of this method causes Color objects
    // to be created! It is possible but a real pain in the rear to avoid using
    // Color objects totally. Requires new methods for the SyntaxUtilities class.
    private void initializeEditorSyntaxStyles() {
        final SyntaxStyle[] syntaxStyle = SyntaxUtilities.getDefaultSyntaxStyles();
        final int tokens = syntaxStyle.length;
        Settings.syntaxStyleColorSettingsKeys = new String[tokens];
        Settings.syntaxStyleBoldSettingsKeys = new String[tokens];
        Settings.syntaxStyleItalicSettingsKeys = new String[tokens];
        Settings.defaultSyntaxStyleColorSettingsValues = new String[tokens];
        Settings.defaultSyntaxStyleBoldSettingsValues = new boolean[tokens];
        Settings.defaultSyntaxStyleItalicSettingsValues = new boolean[tokens];
        this.syntaxStyleColorSettingsValues = new String[tokens];
        this.syntaxStyleBoldSettingsValues = new boolean[tokens];
        this.syntaxStyleItalicSettingsValues = new boolean[tokens];
        for (int i = 0; i < tokens; i++) {
            Settings.syntaxStyleColorSettingsKeys[i] = Settings.SYNTAX_STYLE_COLOR_PREFIX + i;
            Settings.syntaxStyleBoldSettingsKeys[i] = Settings.SYNTAX_STYLE_BOLD_PREFIX + i;
            Settings.syntaxStyleItalicSettingsKeys[i] = Settings.SYNTAX_STYLE_ITALIC_PREFIX + i;
            this.syntaxStyleColorSettingsValues[i] = Settings.defaultSyntaxStyleColorSettingsValues[i] = syntaxStyle[i]
                    .getColorAsHexString();
            this.syntaxStyleBoldSettingsValues[i] = Settings.defaultSyntaxStyleBoldSettingsValues[i] = syntaxStyle[i].isBold();
            this.syntaxStyleItalicSettingsValues[i] = Settings.defaultSyntaxStyleItalicSettingsValues[i] = syntaxStyle[i].isItalic();
        }
    }

    private void getEditorSyntaxStyleSettingsFromPreferences() {
        for (int i = 0; i < Settings.syntaxStyleColorSettingsKeys.length; i++) {
            this.syntaxStyleColorSettingsValues[i] = this.preferences.get(Settings.syntaxStyleColorSettingsKeys[i],
                    this.syntaxStyleColorSettingsValues[i]);
            this.syntaxStyleBoldSettingsValues[i] = this.preferences.getBoolean(Settings.syntaxStyleBoldSettingsKeys[i],
                    this.syntaxStyleBoldSettingsValues[i]);
            this.syntaxStyleItalicSettingsValues[i] = this.preferences.getBoolean(Settings.syntaxStyleItalicSettingsKeys[i],
                    this.syntaxStyleItalicSettingsValues[i]);
        }
    }

    /**
     * Fetch value of a boolean setting given its identifier.
     *
     * @param setting the setting to fetch the value of
     * @return corresponding boolean setting.
     * @throws java.lang.IllegalArgumentException if identifier is invalid.
     */
    public boolean getBooleanSetting(final Bool setting) {
        if (this.booleanSettingsValues.containsKey(setting)) {
            return this.booleanSettingsValues.get(setting);
        } else {
            throw new IllegalArgumentException("Invalid boolean setting ID");
        }
    }

    /**
     * Name of currently selected exception handler file.
     *
     * @return String pathname of current exception handler file, empty if none.
     */
    public String getExceptionHandler() {
        return this.stringSettingsValues[Settings.EXCEPTION_HANDLER];
    }

    /**
     * Set name of exception handler file and write it to persistent storage.
     *
     * @param newFilename name of exception handler file
     */
    public void setExceptionHandler(final String newFilename) {
        this.setStringSetting(Settings.EXCEPTION_HANDLER, newFilename);
    }

    /**
     * Returns identifier of current built-in memory configuration.
     *
     * @return String identifier of current built-in memory configuration, empty if
     * none.
     */
    public String getMemoryConfiguration() {
        return this.stringSettingsValues[Settings.MEMORY_CONFIGURATION];
    }

    /**
     * Store the identifier of the memory configuration.
     *
     * @param config A string that identifies the current built-in memory
     *               configuration
     */
    public void setMemoryConfiguration(final String config) {
        this.setStringSetting(Settings.MEMORY_CONFIGURATION, config);
    }

    /**
     * Current editor font. Retained for compatibility but replaced
     * by: getFontByPosition(Settings.EDITOR_FONT)
     *
     * @return Font object for current editor font.
     */
    public Font getEditorFont() {
        return this.getFontByPosition(Settings.EDITOR_FONT);
    }

    /**
     * Set editor font to the specified Font object and write it to persistent
     * storage.
     * This method retained for compatibility but replaced by:
     * setFontByPosition(Settings.EDITOR_FONT, font)
     *
     * @param font Font object to be used by text editor.
     */
    public void setEditorFont(final Font font) {
        this.setFontByPosition(Settings.EDITOR_FONT, font);
    }

    /**
     * Retrieve a Font setting
     *
     * @param fontSettingPosition constant that identifies which item
     * @return Font object for given item
     */
    public Font getFontByPosition(final int fontSettingPosition) {
        if (fontSettingPosition >= 0 && fontSettingPosition < this.fontFamilySettingsValues.length) {
            return EditorFont.createFontFromStringValues(this.fontFamilySettingsValues[fontSettingPosition],
                    this.fontStyleSettingsValues[fontSettingPosition],
                    this.fontSizeSettingsValues[fontSettingPosition]);
        } else {
            return null;
        }
    }

    /**
     * Order of text segment display columns (there are 5, numbered 0 to 4).
     *
     * @return Array of int indicating the order. Original order is 0 1 2 3 4.
     */
    public int[] getTextColumnOrder() {
        return Settings.getTextSegmentColumnOrder(this.stringSettingsValues[Settings.TEXT_COLUMN_ORDER]);
    }

    /**
     * Store the current order of Text Segment window table columns, so the ordering
     * can be preserved and restored.
     *
     * @param columnOrder An array of int indicating column order.
     */
    public void setTextColumnOrder(final int[] columnOrder) {
        final StringBuilder stringifiedOrder = new StringBuilder();
        for (final int column : columnOrder) {
            stringifiedOrder.append(column).append(" ");
        }
        this.setStringSetting(Settings.TEXT_COLUMN_ORDER, stringifiedOrder.toString());

    }

    /**
     * Retrieve the caret blink rate in milliseconds. Blink rate of 0 means
     * do not blink.
     *
     * @return int blink rate in milliseconds
     */
    public int getCaretBlinkRate() {
        int rate;
        try {
            rate = Integer.parseInt(this.stringSettingsValues[Settings.CARET_BLINK_RATE]);
        } catch (final NumberFormatException nfe) {
            rate = Integer.parseInt(Settings.defaultStringSettingsValues[Settings.CARET_BLINK_RATE]);
        }
        return rate;
    }

    /**
     * Set the caret blinking rate in milliseconds. Rate of 0 means no blinking.
     *
     * @param rate blink rate in milliseconds
     */
    public void setCaretBlinkRate(final int rate) {
        this.setStringSetting(Settings.CARET_BLINK_RATE, "" + rate);
    }

    /**
     * Get the tab size in characters.
     *
     * @return tab size in characters.
     */
    public int getEditorTabSize() {
        int size;
        try {
            size = Integer.parseInt(this.stringSettingsValues[Settings.EDITOR_TAB_SIZE]);
        } catch (final NumberFormatException nfe) {
            size = Settings.getDefaultEditorTabSize();
        }
        return size;
    }

    ////////////////////////////////////////////////////////////////////////
    // Setting Setters
    ////////////////////////////////////////////////////////////////////////

    /**
     * Set the tab size in characters.
     *
     * @param size tab size in characters.
     */
    public void setEditorTabSize(final int size) {
        this.setStringSetting(Settings.EDITOR_TAB_SIZE, "" + size);
    }

    /**
     * Get number of letters to be matched by editor's instruction guide before
     * popup generated (if popup enabled).
     * Should be 1 or 2. If 1, the popup will be generated after first letter typed,
     * based on all matches; if 2,
     * the popup will be generated after second letter typed.
     *
     * @return number of letters (should be 1 or 2).
     */
    public int getEditorPopupPrefixLength() {
        int length = 2;
        try {
            length = Integer.parseInt(this.stringSettingsValues[Settings.EDITOR_POPUP_PREFIX_LENGTH]);
        } catch (final NumberFormatException ignored) {

        }
        return length;
    }

    /**
     * Set number of letters to be matched by editor's instruction guide before
     * popup generated (if popup enabled).
     * Should be 1 or 2. If 1, the popup will be generated after first letter typed,
     * based on all matches; if 2,
     * the popup will be generated after second letter typed.
     *
     * @param length of letters (should be 1 or 2).
     */
    public void setEditorPopupPrefixLength(final int length) {
        this.setStringSetting(Settings.EDITOR_POPUP_PREFIX_LENGTH, "" + length);
    }

    /**
     * Get the saved state of the Labels Window sorting (can sort by either
     * label or address and either ascending or descending order).
     * Default state is 0, by ascending addresses.
     *
     * @return State value 0-7, as a String.
     */
    public String getLabelSortState() {
        return this.stringSettingsValues[Settings.LABEL_SORT_STATE];
    }

    /**
     * Store the current state of the Labels Window sorter. There are 8 possible
     * states
     * as described in LabelsWindow.java
     *
     * @param state The current labels window sorting state, as a String.
     */
    public void setLabelSortState(final String state) {
        this.setStringSetting(Settings.LABEL_SORT_STATE, state);
    }

    /**
     * Get Color object for specified settings key.
     * Returns null if key is not found or its value is not a valid color encoding.
     *
     * @param key the Setting key
     * @return corresponding Color, or null if key not found or value not valid
     * color
     */
    public Color getColorSettingByKey(final String key) {
        return this.getColorValueByKey(key, this.colorSettingsValues, Settings.defaultColorSettingsValues, this.systemColors);
    }

    /**
     * Get default Color value for specified settings key.
     * Returns null if key is not found or its value is not a valid color encoding.
     *
     * @param key the Setting key
     * @return corresponding default Color, or null if key not found or value not
     * valid color
     */
    public Color getDefaultColorSettingByKey(final String key) {
        return this.getColorValueByKey(key, Settings.defaultColorSettingsValues, null, null);
    }

    /**
     * Get Color object for specified settings name (a static constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @return corresponding Color, or null if argument invalid or value not valid
     * color
     */
    public Color getColorSettingByPosition(final int position) {
        return this.getColorValueByPosition(position, this.colorSettingsValues, Settings.defaultColorSettingsValues, this.systemColors);
    }

    /**
     * Get default Color object for specified settings name (a static constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @return corresponding default Color, or null if argument invalid or value not
     * valid color
     */
    public Color getDefaultColorSettingByPosition(final int position) {
        return this.getColorValueByPosition(position, Settings.defaultColorSettingsValues, null, null);
    }

    /**
     * Get Color-Mode for specified settings name (a static constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @return The corresponding color-mode
     */
    public ColorMode getColorModeByPosition(final int position) {
        return Settings.getColorModeByPostion(position, this.colorSettingsValues);
    }

    /**
     * Get a preview of the color-mode for specified settings name (a static
     * constant).
     * Returns null if argument invalid or its value is not a valid color encoding.
     *
     * @param position the Setting name (see list of static constants)
     * @param mode     The color-mode to preview
     * @return The color to preview
     */
    public Color previewColorModeByPosition(final int position, final ColorMode mode) {
        if (mode.modeKey != null && !mode.modeKey.isEmpty()) {
            return Settings.getColorValueByString(position, mode.modeKey, Settings.defaultColorSettingsValues, this.systemColors);
        } else {
            return this.getColorSettingByPosition(position);
        }
    }

    /**
     * Set value of a boolean setting given its id and the value.
     *
     * @param setting setting to set the value of
     * @param value   boolean value to store
     * @throws java.lang.IllegalArgumentException if identifier is not valid.
     */
    public void setBooleanSetting(final Bool setting, final boolean value) {
        if (this.booleanSettingsValues.containsKey(setting)) {
            this.internalSetBooleanSetting(setting, value);
        } else {
            throw new IllegalArgumentException("Invalid boolean setting ID");
        }
    }

    /**
     * Temporarily establish boolean setting. This setting will NOT be written to
     * persisent
     * store! Currently this is used only when running RARS from the command line
     *
     * @param setting the setting to set the value of
     * @param value   True to enable the setting, false otherwise.
     */
    public void setBooleanSettingNonPersistent(final Bool setting, final boolean value) {
        if (this.booleanSettingsValues.containsKey(setting)) {
            this.booleanSettingsValues.put(setting, value);
        } else {
            throw new IllegalArgumentException("Invalid boolean setting ID");
        }
    }

    /**
     * Store a Font setting
     *
     * @param fontSettingPosition Constant that identifies the item the font goes
     *                            with
     * @param font                The font to set that item to
     */
    public void setFontByPosition(final int fontSettingPosition, final Font font) {
        if (fontSettingPosition >= 0 && fontSettingPosition < this.fontFamilySettingsValues.length) {
            this.fontFamilySettingsValues[fontSettingPosition] = font.getFamily();
            this.fontStyleSettingsValues[fontSettingPosition] = EditorFont.styleIntToStyleString(font.getStyle());
            this.fontSizeSettingsValues[fontSettingPosition] = EditorFont.sizeIntToSizeString(font.getSize());
            this.saveFontSetting(fontSettingPosition, Settings.fontFamilySettingsKeys, this.fontFamilySettingsValues);
            this.saveFontSetting(fontSettingPosition, Settings.fontStyleSettingsKeys, this.fontStyleSettingsValues);
            this.saveFontSetting(fontSettingPosition, Settings.fontSizeSettingsKeys, this.fontSizeSettingsValues);
        }
        this.submit(SettingsNotice.get());
    }

    /**
     * Set Color object for specified settings key. Has no effect if key is invalid.
     *
     * @param key   the Setting key
     * @param color the Color to save
     */
    public void setColorSettingByKey(final String key, final Color color) {
        for (int i = 0; i < Settings.colorSettingsKeys.length; i++) {
            if (key.equals(Settings.colorSettingsKeys[i])) {
                this.setColorSettingByPosition(i, color);
                return;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE HELPER METHODS TO DO THE REAL WORK
    //
    /////////////////////////////////////////////////////////////////////////

    // Initialize settings to default values.
    // Strategy: First set from properties file.
    // If that fails, set from array.
    // In either case, use these values as defaults in call to Preferences.

    /**
     * Set Color object for specified settings name (a static constant). Has no
     * effect if invalid.
     *
     * @param position the Setting name (see list of static constants)
     * @param color    the Color to save
     */
    public void setColorSettingByPosition(final int position, final Color color) {
        this.setColorSetting(position, color);
    }

    /**
     * Set Color-Mode for specified settings name (a static constant). Has no effect
     * if invalid.
     *
     * @param position the Setting name (see list of static constants)
     * @param mode     the color-mode to set
     */
    public void setColorSettingByPosition(final int position, final ColorMode mode) {
        this.setColorSetting(position, mode.modeKey);
    }

    /**
     * <p>Getter for the field <code>defaultColorMode</code>.</p>
     *
     * @return Default color mode
     */
    public ColorMode getDefaultColorMode() {
        return this.defaultColorMode;
    }

    private void initialize() {
        this.initSystemProviders();
        this.applyDefaultSettings();
        if (!this.readSettingsFromPropertiesFile(Settings.settingsFile)) {
            Settings.LOGGER.error("RARS System error: unable to read Settings.properties defaults. Using built-in defaults.");
        }
        this.getSettingsFromPreferences();
    }

    // Default values. Will be replaced if available from property file or
    // Preferences object.
    private void applyDefaultSettings() {
        for (final Bool setting : Bool.values()) {
            this.booleanSettingsValues.put(setting, setting.getDefault());
        }
        System.arraycopy(Settings.defaultStringSettingsValues, 0, this.stringSettingsValues, 0, this.stringSettingsValues.length);
        for (int i = 0; i < this.fontFamilySettingsValues.length; i++) {
            this.fontFamilySettingsValues[i] = Settings.defaultFontFamilySettingsValues[i];
            this.fontStyleSettingsValues[i] = Settings.defaultFontStyleSettingsValues[i];
            this.fontSizeSettingsValues[i] = Settings.defaultFontSizeSettingsValues[i];
        }
        Arrays.fill(this.colorSettingsValues, this.getDefaultColorMode().modeKey);
        this.initializeEditorSyntaxStyles();
    }

    private void initSystemProviders() {
        this.systemColors = new SystemColorProvider[Settings.colorSettingsKeys.length];
        this.systemColors[Settings.EDITOR_BACKGROUND] = new LookAndFeelColor("TextArea.background");
        this.systemColors[Settings.EDITOR_FOREGROUND] = new LookAndFeelColor("TextArea.foreground");
        this.systemColors[Settings.EDITOR_SELECTION_COLOR] = new LookAndFeelColor("TextArea.selectionBackground");
        this.systemColors[Settings.EDITOR_CARET_COLOR] = new LookAndFeelColor("TextArea.caretForeground");
        // Mixes based on the system-color of the background and selection-color
        this.systemColors[Settings.EDITOR_LINE_HIGHLIGHT] = new ColorProviderMix(this.systemColors[Settings.EDITOR_SELECTION_COLOR],
                this.systemColors[Settings.EDITOR_BACKGROUND], 0.2f);
        // Mixes based on the set color of the background and selection-color
        // systemColors[EDITOR_LINE_HIGHLIGHT] = new
        // ColorSettingMix(EDITOR_SELECTION_COLOR, EDITOR_BACKGROUND, 0.2f);
    }

    // Used by all the boolean setting "setter" methods.
    private void internalSetBooleanSetting(final Bool setting, final boolean value) {
        if (value != this.booleanSettingsValues.get(setting)) {
            this.booleanSettingsValues.put(setting, value);
            this.saveBooleanSetting(setting.getName(), value);
            this.submit(SettingsNotice.get());
        }
    }

    // Used by setter method(s) for string-based settings (initially, only exception
    // handler name)
    private void setStringSetting(final int settingIndex, final String value) {
        this.stringSettingsValues[settingIndex] = value;
        this.saveStringSetting(settingIndex);
    }

    // Used by setter methods for color-based settings
    private void setColorSetting(final int settingIndex, final Color color) {
        this.setColorSetting(settingIndex,
                Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue()));
    }

    private void setColorSetting(final int settingIndex, final String colorStr) {
        if (settingIndex >= 0 && settingIndex < this.colorSettingsValues.length) {
            this.colorSettingsValues[settingIndex] = colorStr;
            this.saveColorSetting(settingIndex);
        }
    }

    // Get Color object for this key value. Get it from values array provided as
    // argument (could be either
    // the current or the default settings array).
    private Color getColorValueByKey(final String key, final String[] values, final String[] defaults, final SystemColorProvider[] system) {
        for (int i = 0; i < Settings.colorSettingsKeys.length; i++) {
            if (key.equals(Settings.colorSettingsKeys[i])) {
                return this.getColorValueByPosition(i, values, defaults, system);
            }
        }
        return null;
    }

    // Get Color object for this key array position. Get it from values array
    // provided as argument (could be either
    // the current or the default settings array).
    private Color getColorValueByPosition(final int position, final String[] values, final String[] defaults,
                                          final SystemColorProvider[] system) {
        return Settings.getColorValueByString(position, Settings.getColorStringByPosition(position, values), defaults, this.systemColors);
    }

    // Establish the settings from the given properties file. Return true if it
    // worked,
    // false if it didn't. Note the properties file exists only to provide default
    // values
    // in case the Preferences fail or have not been recorded yet.
    //
    // Any settings successfully read will be stored in both the xSettingsValues and
    // defaultXSettingsValues arrays (x=boolean,string,color). The latter will
    // overwrite the
    // last-resort default values hardcoded into the arrays above.
    //
    // NOTE: If there is NO ENTRY for the specified property,
    // Globals.getPropertyEntry() returns
    // null. This is no cause for alarm. It will occur during system development or
    // upon the
    // first use of a new RARS release in which new settings have been defined.
    // In that case, this method will NOT make an assignment to the settings array!
    // So consider it a precondition of this method: the settings arrays must
    // already be
    // initialized with last-resort default values.
    private boolean readSettingsFromPropertiesFile(final String filename) {
        String settingValue;
        try {
            for (final Bool setting : Bool.values()) {
                settingValue = Globals.getPropertyEntry(filename, setting.getName());
                if (settingValue != null) {
                    final boolean value = Boolean.parseBoolean(settingValue);
                    setting.setDefault(value);
                    this.booleanSettingsValues.put(setting, value);
                }
            }
            for (int i = 0; i < Settings.stringSettingsKeys.length; i++) {
                settingValue = Globals.getPropertyEntry(filename, Settings.stringSettingsKeys[i]);
                if (settingValue != null)
                    this.stringSettingsValues[i] = Settings.defaultStringSettingsValues[i] = settingValue;
            }
            for (int i = 0; i < this.fontFamilySettingsValues.length; i++) {
                settingValue = Globals.getPropertyEntry(filename, Settings.fontFamilySettingsKeys[i]);
                if (settingValue != null)
                    this.fontFamilySettingsValues[i] = Settings.defaultFontFamilySettingsValues[i] = settingValue;
                settingValue = Globals.getPropertyEntry(filename, Settings.fontStyleSettingsKeys[i]);
                if (settingValue != null)
                    this.fontStyleSettingsValues[i] = Settings.defaultFontStyleSettingsValues[i] = settingValue;
                settingValue = Globals.getPropertyEntry(filename, Settings.fontSizeSettingsKeys[i]);
                if (settingValue != null)
                    this.fontSizeSettingsValues[i] = Settings.defaultFontSizeSettingsValues[i] = settingValue;
            }
            for (int i = 0; i < Settings.colorSettingsKeys.length; i++) {
                settingValue = Globals.getPropertyEntry(filename, Settings.colorSettingsKeys[i]);
                if (settingValue != null)
                    this.colorSettingsValues[i] = Settings.defaultColorSettingsValues[i] = settingValue;
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    // Get settings values from Preferences object. A key-value pair will only be
    // written
    // to Preferences if/when the value is modified. If it has not been modified,
    // the default value
    // will be returned here.
    //
    // PRECONDITION: Values arrays have already been initialized to default values
    // from
    // Settings.properties file or default value arrays above!
    private void getSettingsFromPreferences() {
        this.booleanSettingsValues.replaceAll(
                (s, v) -> this.preferences.getBoolean(s.getName(), this.booleanSettingsValues.get(s)));
        for (int i = 0; i < Settings.stringSettingsKeys.length; i++) {
            this.stringSettingsValues[i] = this.preferences.get(Settings.stringSettingsKeys[i], this.stringSettingsValues[i]);
        }
        for (int i = 0; i < Settings.fontFamilySettingsKeys.length; i++) {
            this.fontFamilySettingsValues[i] = this.preferences.get(Settings.fontFamilySettingsKeys[i], this.fontFamilySettingsValues[i]);
            this.fontStyleSettingsValues[i] = this.preferences.get(Settings.fontStyleSettingsKeys[i], this.fontStyleSettingsValues[i]);
            this.fontSizeSettingsValues[i] = this.preferences.get(Settings.fontSizeSettingsKeys[i], this.fontSizeSettingsValues[i]);
        }
        for (int i = 0; i < Settings.colorSettingsKeys.length; i++) {
            this.colorSettingsValues[i] = this.preferences.get(Settings.colorSettingsKeys[i], this.colorSettingsValues[i]);
        }
        this.getEditorSyntaxStyleSettingsFromPreferences();
    }

    // Save the key-value pair in the Properties object and assure it is written to
    // persisent storage.
    private void saveBooleanSetting(final String name, final boolean value) {
        try {
            this.preferences.putBoolean(name, value);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write the boolean setting {} to persistent storage for security reasons.", name);
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage when trying to write the \"{}\" bool setting.", name);
        }
    }

    // Save the key-value pair in the Properties object and assure it is written to
    // persisent storage.
    private void saveStringSetting(final int index) {
        var name = Settings.stringSettingsKeys[index];
        try {
            this.preferences.put(name, this.stringSettingsValues[index]);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write the {} string setting to persistent storage for security reasons.", name);
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage when trying to write the \"{}\" string setting.", name);
        }
    }

    // Save the key-value pair in the Properties object and assure it is written to
    // persisent storage.
    private void saveFontSetting(final int index, final String[] settingsKeys, final String[] settingsValues) {
        try {
            this.preferences.put(settingsKeys[index], settingsValues[index]);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write the font settings to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage when trying to write the font settings.");
        }
    }

    // Save the key-value pair in the Properties object and assure it is written to
    // persisent storage.
    private void saveColorSetting(final int index) {
        try {
            this.preferences.put(Settings.colorSettingsKeys[index], this.colorSettingsValues[index]);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write the color settings to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage when trying to write the color settings.");
        }
    }

    // BOOLEAN SETTINGS...
    public enum Bool {
        /**
         * Flag to determine whether or not program being assembled is limited to
         * basic instructions and formats.
         */
        EXTENDED_ASSEMBLER_ENABLED("ExtendedAssembler", true),
        /**
         * Flag to determine whether or not a file is immediately and automatically
         * assembled
         * upon opening. Handy when using externa editor like mipster.
         */
        ASSEMBLE_ON_OPEN("AssembleOnOpen", false),
        /**
         * Flag to determine whether all files open currently source file will be
         * assembled when assembly is selected.
         */
        ASSEMBLE_OPEN("AssembleOpen", false),
        /**
         * Flag to determine whether files in the directory of the current source file
         * will be assembled when assembly is selected.
         */
        ASSEMBLE_ALL("AssembleAll", false),

        /**
         * Default visibilty of label window (symbol table). Default only, dynamic
         * status
         * maintained by ExecutePane
         */
        LABEL_WINDOW_VISIBILITY("LabelWindowVisibility", false),
        /**
         * Default setting for displaying addresses and values in hexidecimal in the
         * Execute
         * pane.
         */
        DISPLAY_ADDRESSES_IN_HEX("DisplayAddressesInHex", true),
        DISPLAY_VALUES_IN_HEX("DisplayValuesInHex", true),
        /**
         * Flag to determine whether the currently selected exception handler source
         * file will
         * be included in each assembly operation.
         */
        EXCEPTION_HANDLER_ENABLED("LoadExceptionHandler", false),
        /**
         * Flag to determine whether or not the editor will display line numbers.
         */
        EDITOR_LINE_NUMBERS_DISPLAYED("EditorLineNumbersDisplayed", true),
        /**
         * Flag to determine whether or not assembler warnings are considered errors.
         */
        WARNINGS_ARE_ERRORS("WarningsAreErrors", false),
        /**
         * Flag to determine whether or not to display and use program arguments
         */
        PROGRAM_ARGUMENTS("ProgramArguments", false),
        /**
         * Flag to control whether or not highlighting is applied to data segment window
         */
        DATA_SEGMENT_HIGHLIGHTING("DataSegmentHighlighting", true),
        /**
         * Flag to control whether or not highlighting is applied to register windows
         */
        REGISTERS_HIGHLIGHTING("RegistersHighlighting", true),
        /**
         * Flag to control whether or not assembler automatically initializes program
         * counter to 'main's address
         */
        START_AT_MAIN("StartAtMain", false),
        /**
         * Flag to control whether or not editor will highlight the line currently being
         * edited
         */
        EDITOR_CURRENT_LINE_HIGHLIGHTING("EditorCurrentLineHighlighting", true),
        /**
         * Flag to control whether or not editor will provide popup instruction guidance
         * while typing
         */
        POPUP_INSTRUCTION_GUIDANCE("PopupInstructionGuidance", true),
        /**
         * Flag to control whether or not simulator will use popup dialog for input
         * syscalls
         */
        POPUP_SYSCALL_INPUT("PopupSyscallInput", false),
        /**
         * Flag to control whether or not language-aware editor will use auto-indent
         * feature
         */
        AUTO_INDENT("AutoIndent", true),
        /**
         * Flag to determine whether a program can write binary code to the text or data
         * segment and
         * execute that code.
         */
        SELF_MODIFYING_CODE_ENABLED("SelfModifyingCode", false),
        /**
         * Flag to determine whether a program uses rv64i instead of rv32i
         */
        RV64_ENABLED("rv64Enabled", false),
        /**
         * Flag to determine whether to calculate relative paths from the current
         * working directory
         * or from the RARS executable path.
         */
        DERIVE_CURRENT_WORKING_DIRECTORY("DeriveCurrentWorkingDirectory", false);

        // TODO: add option for turning off user trap handling and interrupts
        private final @NotNull String name;
        private boolean value;

        Bool(final @NotNull String n, final boolean v) {
            this.name = n;
            this.value = v;
        }

        boolean getDefault() {
            return this.value;
        }

        void setDefault(final boolean v) {
            this.value = v;
        }

        @NotNull String getName() {
            return this.name;
        }
    }

    public enum ColorMode {
        DEFAULT("DEF"),
        SYSTEM("SYS"),
        CUSTOM(null);

        public final @Nullable String modeKey;

        ColorMode(final @Nullable String modeKey) {
            this.modeKey = modeKey;
        }
    }

    interface SystemColorProvider {
        Color getColor();
    }

    /**
     * Takes a color from the LookAndFeel
     */
    static class LookAndFeelColor implements SystemColorProvider {
        private final String key;

        public LookAndFeelColor(final String key) {
            this.key = key;
        }

        @Override
        public Color getColor() {
            // Deep copy, because using the color directly in UI caused problems
            return new Color(UIManager.getLookAndFeel().getDefaults().getColor(this.key).getRGB());
        }
    }

    /**
     * Mixes color of two providers
     */
    static class ColorProviderMix implements SystemColorProvider {
        private final SystemColorProvider proA;
        private final SystemColorProvider proB;
        private final float ratio;

        public ColorProviderMix(final SystemColorProvider proA, final SystemColorProvider proB, final float ratio) {
            this.proA = proA;
            this.proB = proB;
            this.ratio = ratio;
        }

        @Override
        public Color getColor() {
            return Settings.mixColors(this.proA.getColor(), this.proB.getColor(), this.ratio);
        }
    }

    /**
     * Mixes two other setting-colors
     */
    @SuppressWarnings("unused")
    class ColorSettingMix implements SystemColorProvider {
        private final int posA;
        private final int posB;
        private final float ratio;

        public ColorSettingMix(final int posA, final int posB, final float ratio) {
            this.posA = posA;
            this.posB = posB;
            this.ratio = ratio;
        }

        @Override
        public Color getColor() {
            return Settings.mixColors(Settings.this.getColorSettingByPosition(this.posA), Settings.this.getColorSettingByPosition(this.posB), this.ratio);
        }
    }

}
