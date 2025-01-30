package rars.settings

import rars.Globals
import rars.riscv.hardware.MemoryConfiguration

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