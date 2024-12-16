package rars;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.notices.SettingsNotice;
import rars.settings.*;
import rars.util.CustomPublisher;
import rars.settings.BoolSettings;
import rars.settings.EditorThemeSettings;
import rars.settings.FontSettings;
import rars.settings.RuntimeTableHighlightingSettings;

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
public final class Settings extends CustomPublisher<SettingsNotice> {
    /**
     * Current specified exception handler file (a RISCV assembly source file)
     */
    public static final int EXCEPTION_HANDLER = 0;
    /**
     * State for sorting label window display
     */
    public static final int LABEL_SORT_STATE = 2;
    /**
     * Identifier of current memory configuration
     */
    public static final int MEMORY_CONFIGURATION = 3;

    // STRING SETTINGS. Each array position has associated name.
    /**
     * Caret blink rate in milliseconds, 0 means don't blink.
     */
    public static final int CARET_BLINK_RATE = 4;
    /**
     * Editor tab size in characters.
     */
    public static final int EDITOR_TAB_SIZE = 5;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String[] stringSettingsKeys = {"ExceptionHandler", "TextColumnOrder", "LabelSortState",
            "MemoryConfiguration", "CaretBlinkRate", "EditorTabSize", "EditorPopupPrefixLength"};
    /**
     * Last resort default values for String settings;
     * will use only if neither the Preferences nor the properties file work.
     * If you wish to change, do so before instantiating the Settings object.
     * Must match first by list position.
     */
    private static final String[] defaultStringSettingsValues = {"", "0 1 2 3 4", "0", "", "500", "8", "2"};

    private final String[] stringSettingsValues;
    private final Preferences preferences;
    private final EditorThemeSettings editorThemeSettings;
    private final FontSettings fontSettings;
    private final BoolSettings boolSettings;
    private final RuntimeTableHighlightingSettings runtimeTableHighlightingSettings;

    /**
     * Create Settings object and set to saved values. If saved values not found,
     * will set
     * based on defaults stored in Settings.properties file. If file problems, will
     * set based
     * on defaults stored in this class.
     */
    public Settings() {
        this.stringSettingsValues = new String[Settings.stringSettingsKeys.length];
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
        this.editorThemeSettings = new EditorThemeSettings(this.preferences);
        this.fontSettings = new FontSettings(this.preferences);
        this.boolSettings = new BoolSettings(this.preferences);
        this.runtimeTableHighlightingSettings = new RuntimeTableHighlightingSettings(this.preferences);
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
     * Get the text editor default tab size in characters
     *
     * @return tab size in characters
     */
    public static int getDefaultEditorTabSize() {
        return Integer.parseInt(Settings.defaultStringSettingsValues[Settings.EDITOR_TAB_SIZE]);
    }

    public EditorThemeSettings getEditorThemeSettings() {
        return editorThemeSettings;
    }

    public FontSettings getFontSettings() {
        return fontSettings;
    }
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

    public BoolSettings getBoolSettings() {
        return boolSettings;
    }

    public RuntimeTableHighlightingSettings getRuntimeTableHighlightingSettings() {
        return runtimeTableHighlightingSettings;
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

    /**
     * Set the tab size in characters.
     *
     * @param size tab size in characters.
     */
    public void setEditorTabSize(final int size) {
        this.setStringSetting(Settings.EDITOR_TAB_SIZE, "" + size);
    }

    /**
     * Get the saved state of the Labels Window sorting (can sort by either
     * label or address and either ascending or descending order).
     * Default state is 0, by ascending addresses.
     *
     * @return State second 0-7, as a String.
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

    // Used by setter method(s) for string-based settings (initially, only exception
    // handler name)
    private void setStringSetting(final int settingIndex, final String value) {
        this.stringSettingsValues[settingIndex] = value;
        this.saveStringSetting(settingIndex);
    }

    // Save the first-second pair in the Properties object and assure it is written to
    // persisent storage.
    private void saveStringSetting(final int index) {
        final var name = Settings.stringSettingsKeys[index];
        try {
            this.preferences.put(name, this.stringSettingsValues[index]);
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write the {} string setting to persistent storage for security reasons.", name);
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage when trying to write the \"{}\" string " +
                    "setting.", name);
        }
    }
}
