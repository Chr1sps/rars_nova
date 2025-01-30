package rars.settings

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.Globals
import rars.riscv.hardware.MemoryConfiguration
import rars.util.ListenerDispatcher
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

interface OtherSettings {
    companion object {
        /**
         * Return whether backstepping is permitted at this time. Backstepping is
         * ability to undo execution
         * steps one at a time. Available only in the IDE. This is not a persistent
         * setting and is not under
         * RARS user control.
         *
         * @return true if backstepping is permitted, false otherwise.
         */
        @JvmStatic
        fun getBackSteppingEnabled(): Boolean = Globals.program?.backStepper?.enabled() == true
    }

    val memoryConfiguration: MemoryConfiguration
    val exceptionHandler: String
    val labelSortState: Int
    val editorTabSize: Int
    val caretBlinkRate: Int
}

class OtherSettingsImpl(private val preferences: Preferences) : OtherSettings {
    private val onChangeDispatcher = ListenerDispatcher<Void?>()

    @JvmField
    val onChangeListenerHook = this.onChangeDispatcher.hook

    override var exceptionHandler: String
        private set
    override var memoryConfiguration: MemoryConfiguration
        private set
    override var caretBlinkRate: Int
        private set
    override var editorTabSize: Int
        private set
    override var labelSortState: Int
        private set

    init {
        labelSortState = preferences.getInt(OTHER_PREFIX + SORT_STATE, 0)
        memoryConfiguration = loadMemoryConfiguration()
        caretBlinkRate = preferences.getInt(OTHER_PREFIX + CARET_BLINK_RATE, 500)
        editorTabSize = preferences.getInt(OTHER_PREFIX + EDITOR_TAB_SIZE, 4)
        exceptionHandler = preferences.get(OTHER_PREFIX + EXCEPTION_HANDLER, "")
    }

    fun setMemoryConfigurationAndSave(memoryConfiguration: MemoryConfiguration) {
        if (this.memoryConfiguration != memoryConfiguration) {
            this.memoryConfiguration = memoryConfiguration
            this.preferences.put(OTHER_PREFIX + MEMORY_CONFIGURATION, this.memoryConfiguration.identifier)
            this.commitChanges()
        }
    }

    fun setExceptionHandlerAndSave(exceptionHandler: String) {
        if (this.exceptionHandler != exceptionHandler) {
            this.exceptionHandler = exceptionHandler
            this.preferences.put(OTHER_PREFIX + EXCEPTION_HANDLER, this.exceptionHandler)
            this.commitChanges()
        }
    }

    fun setLabelSortStateAndSave(labelSortState: Int) {
        if (this.labelSortState != labelSortState) {
            this.labelSortState = labelSortState
            this.preferences.putInt(OTHER_PREFIX + SORT_STATE, this.labelSortState)
            this.commitChanges()
        }
    }

    fun setCaretBlinkRateAndSave(caretBlinkRate: Int) {
        if (this.caretBlinkRate != caretBlinkRate) {
            this.caretBlinkRate = caretBlinkRate
            this.preferences.putInt(OTHER_PREFIX + CARET_BLINK_RATE, this.caretBlinkRate)
            this.commitChanges()
        }
    }

    fun setEditorTabSizeAndSave(editorTabSize: Int) {
        if (this.editorTabSize != editorTabSize) {
            this.editorTabSize = editorTabSize
            this.preferences.putInt(OTHER_PREFIX + EDITOR_TAB_SIZE, this.editorTabSize)
            this.commitChanges()
        }
    }

    private fun commitChanges() {
        try {
            this.preferences.flush()
        } catch (_: SecurityException) {
            LOGGER.error("Unable to write to persistent storage for security reasons.")
        } catch (_: BackingStoreException) {
            LOGGER.error("Unable to communicate with persistent storage.")
        }
        this.onChangeDispatcher.dispatch(null)
    }

    private fun loadMemoryConfiguration(): MemoryConfiguration {
        val memoryConfigurationName: String = preferences.get(
            OTHER_PREFIX + MEMORY_CONFIGURATION,
            MemoryConfiguration.DEFAULT.identifier
        )
        return MemoryConfiguration.fromIdString(memoryConfigurationName)!!
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(OtherSettingsImpl::class.java)

        // region Preferences keys
        private const val OTHER_PREFIX = "Other"

        private const val EXCEPTION_HANDLER = "Exception_handler"
        private const val SORT_STATE = "Label_sort_state"
        private const val MEMORY_CONFIGURATION = "Memory_configuration"
        private const val CARET_BLINK_RATE = "Caret_blink_rate"
        private const val EDITOR_TAB_SIZE = "Editor_tab_size"
        // endregion Preferences keys
    }
}