package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.riscv.hardware.MemoryConfiguration;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class OtherSettings extends SettingsBase {
    private static final Logger LOGGER = LogManager.getLogger();

    // region Preferences keys
    private static final String OTHER_PREFIX = "Other";

    private static final String EXCEPTION_HANDLER = "Exception_handler";
    private static final String SORT_STATE = "Label_sort_state";
    private static final String MEMORY_CONFIGURATION = "Memory_configuration";
    private static final String CARET_BLINK_RATE = "Caret_blink_rate";
    private static final String EDITOR_TAB_SIZE = "Editor_tab_size";
    public static @NotNull OtherSettings OTHER_SETTINGS = new OtherSettings(SETTINGS_PREFERENCES);
    // endregion Preferences keys

    private final @NotNull Preferences preferences;
    private @NotNull String /*labelSortState,*/ exceptionHandler;
    private @NotNull MemoryConfiguration memoryConfiguration;

    private int caretBlinkRate, editorTabSize, labelSortState;

    public OtherSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        this.loadSettingsFromPreferences();
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
        return (
            Globals.program != null &&
                Globals.program.getBackStepper() != null &&
                Globals.program.getBackStepper().enabled()
        );
    }

    public void setMemoryConfigurationAndSave(final @NotNull MemoryConfiguration memoryConfiguration) {
        if (!this.memoryConfiguration.equals(memoryConfiguration)) {
            this.memoryConfiguration = memoryConfiguration;
            this.saveSettingsToPreferences();
        }
    }

    public @NotNull MemoryConfiguration getMemoryConfiguration() {
        return memoryConfiguration;
    }

    public void setExceptionHandlerAndSave(final @NotNull String exceptionHandler) {
        if (!this.exceptionHandler.equals(exceptionHandler)) {
            this.exceptionHandler = exceptionHandler;
            this.saveSettingsToPreferences();
        }
    }

    public @NotNull String getExceptionHandler() {
        return exceptionHandler;
    }

    public void setLabelSortStateAndSave(final int labelSortState) {
        if (this.labelSortState != labelSortState) {
            this.labelSortState = labelSortState;
            this.saveSettingsToPreferences();
        }
    }

    public int getLabelSortState() {
        return labelSortState;
    }

    public void setCaretBlinkRateAndSave(final int caretBlinkRate) {
        if (this.caretBlinkRate != caretBlinkRate) {
            this.caretBlinkRate = caretBlinkRate;
            this.saveSettingsToPreferences();
        }
    }

    public int getCaretBlinkRate() {
        return caretBlinkRate;
    }

    public void setEditorTabSizeAndSave(final int editorTabSize) {
        if (this.editorTabSize != editorTabSize) {
            this.editorTabSize = editorTabSize;
            this.saveSettingsToPreferences();
        }
    }

    public int getEditorTabSize() {
        return editorTabSize;
    }

    private void saveSettingsToPreferences() {
        this.preferences.putInt(OTHER_PREFIX + SORT_STATE, this.labelSortState);
        this.preferences.put(OTHER_PREFIX + MEMORY_CONFIGURATION, this.memoryConfiguration.identifier);
        this.preferences.putInt(OTHER_PREFIX + CARET_BLINK_RATE, this.caretBlinkRate);
        this.preferences.putInt(OTHER_PREFIX + EDITOR_TAB_SIZE, this.editorTabSize);
        this.preferences.put(OTHER_PREFIX + EXCEPTION_HANDLER, this.exceptionHandler);
        commitChanges();
    }

    private void commitChanges() {
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException e) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
        submit();
    }

    // region Preference loading methods
    private void loadSettingsFromPreferences() {
        this.labelSortState = preferences.getInt(OTHER_PREFIX + SORT_STATE, 0);
        this.memoryConfiguration = loadMemoryConfiguration();
        this.caretBlinkRate = preferences.getInt(OTHER_PREFIX + CARET_BLINK_RATE, 500);
        this.editorTabSize = preferences.getInt(OTHER_PREFIX + EDITOR_TAB_SIZE, 4);
        this.exceptionHandler = preferences.get(OTHER_PREFIX + EXCEPTION_HANDLER, "");
    }

    private @NotNull MemoryConfiguration loadMemoryConfiguration() {
        final var memoryConfigurationName = preferences.get(
            OTHER_PREFIX + MEMORY_CONFIGURATION,
            MemoryConfiguration.DEFAULT.identifier
        );
        // noinspection DataFlowIssue
        return MemoryConfiguration.fromIdString(memoryConfigurationName);
    }
    // endregion Preference loading methods
}
