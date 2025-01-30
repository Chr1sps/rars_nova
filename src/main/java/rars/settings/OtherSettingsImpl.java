package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.riscv.hardware.MemoryConfiguration;
import rars.util.ListenerDispatcher;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class OtherSettingsImpl implements OtherSettings {
    private static final @NotNull Logger LOGGER = LogManager.getLogger(OtherSettingsImpl.class);

    // region Preferences keys

    private static final String OTHER_PREFIX = "Other";

    private static final String EXCEPTION_HANDLER = "Exception_handler";
    private static final String SORT_STATE = "Label_sort_state";
    private static final String MEMORY_CONFIGURATION = "Memory_configuration";
    private static final String CARET_BLINK_RATE = "Caret_blink_rate";
    private static final String EDITOR_TAB_SIZE = "Editor_tab_size";

    // endregion Preferences keys

    public final @NotNull ListenerDispatcher<Void>.Hook onChangeListenerHook;
    private final @NotNull ListenerDispatcher<Void> onChangeDispatcher;
    private final @NotNull Preferences preferences;
    private @NotNull String /*labelSortState,*/ exceptionHandler;
    private @NotNull MemoryConfiguration memoryConfiguration;

    private int caretBlinkRate, editorTabSize, labelSortState;

    public OtherSettingsImpl(final @NotNull Preferences preferences) {
        this.onChangeDispatcher = new ListenerDispatcher<>();
        this.onChangeListenerHook = this.onChangeDispatcher.getHook();
        this.preferences = preferences;
        this.loadSettingsFromPreferences();
    }

    public void setMemoryConfigurationAndSave(final @NotNull MemoryConfiguration memoryConfiguration) {
        if (!this.memoryConfiguration.equals(memoryConfiguration)) {
            this.memoryConfiguration = memoryConfiguration;
            this.preferences.put(OTHER_PREFIX + MEMORY_CONFIGURATION, this.memoryConfiguration.identifier);
            this.commitChanges();
        }
    }

    @Override
    public @NotNull MemoryConfiguration getMemoryConfiguration() {
        return memoryConfiguration;
    }

    public void setExceptionHandlerAndSave(final @NotNull String exceptionHandler) {
        if (!this.exceptionHandler.equals(exceptionHandler)) {
            this.exceptionHandler = exceptionHandler;
            this.preferences.put(OTHER_PREFIX + EXCEPTION_HANDLER, this.exceptionHandler);
            this.commitChanges();
        }
    }

    @Override
    public @NotNull String getExceptionHandler() {
        return exceptionHandler;
    }

    public void setLabelSortStateAndSave(final int labelSortState) {
        if (this.labelSortState != labelSortState) {
            this.labelSortState = labelSortState;
            this.preferences.putInt(OTHER_PREFIX + SORT_STATE, this.labelSortState);
            this.commitChanges();
        }
    }

    @Override
    public int getLabelSortState() {
        return labelSortState;
    }

    public void setCaretBlinkRateAndSave(final int caretBlinkRate) {
        if (this.caretBlinkRate != caretBlinkRate) {
            this.caretBlinkRate = caretBlinkRate;
            this.preferences.putInt(OTHER_PREFIX + CARET_BLINK_RATE, this.caretBlinkRate);
            this.commitChanges();
        }
    }

    @Override
    public int getCaretBlinkRate() {
        return caretBlinkRate;
    }

    public void setEditorTabSizeAndSave(final int editorTabSize) {
        if (this.editorTabSize != editorTabSize) {
            this.editorTabSize = editorTabSize;
            this.preferences.putInt(OTHER_PREFIX + EDITOR_TAB_SIZE, this.editorTabSize);
            this.commitChanges();
        }
    }

    @Override
    public int getEditorTabSize() {
        return editorTabSize;
    }

    private void commitChanges() {
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException e) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
        this.onChangeDispatcher.dispatch(null);
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
