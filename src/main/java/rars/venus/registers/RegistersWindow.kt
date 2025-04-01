package rars.venus.registers

import rars.riscv.hardware.registerFiles.RegisterFile
import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.venus.NumberDisplayBaseChooser
import rars.venus.VenusUI

class RegistersWindow(
    registerFile: RegisterFile,
    mainUI: VenusUI,
    settings: AllSettings,
) : RegisterBlockWindowBase(
    registerFile,
    regToolTips,
    "Current 32 bit value",
    mainUI,
    settings
) {
    override fun formatRegisterValue(value: Long, base: Int): String =
        if (settings.boolSettings.getSetting(BoolSetting.RV64_ENABLED)) {
            NumberDisplayBaseChooser.formatNumber(value, base)
        } else {
            NumberDisplayBaseChooser.formatNumber(value.toInt(), base)
        }

    companion object {
        const val SAVED_TEMPORARY = "saved temporary (preserved across call)"
        const val TEMPORARY = "temporary (not preserved across call)"
        
        /** The tips to show when hovering over the names of the registers */
        private val regToolTips = arrayOf(
            "constant 0", // zero 
            "return address (used by function call)", // ra 
            "stack pointer", // sp 
            "pointer to global area", // gp 
            "pointer to thread local data (not given a value)", // tp 
            TEMPORARY, // t0 
            TEMPORARY, // t1 
            TEMPORARY, // t2 
            SAVED_TEMPORARY, // s0 
            SAVED_TEMPORARY, // s1 
            "argument 1 / return 1", // a0 
            "argument 2 / return 2", // a1 
            "argument 3", // a2 
            "argument 4", // a3 
            "argument 5", // a4 
            "argument 6", // a5 
            "argument 7", // a6 
            "argument 8", // a7 
            SAVED_TEMPORARY, // s2 
            SAVED_TEMPORARY, // s3 
            SAVED_TEMPORARY, // s4 
            SAVED_TEMPORARY, // s5 
            SAVED_TEMPORARY, // s6 
            SAVED_TEMPORARY, // s7 
            SAVED_TEMPORARY, // s8 
            SAVED_TEMPORARY, // s9 
            SAVED_TEMPORARY, // s10 
            SAVED_TEMPORARY, // s11 
            TEMPORARY, // t3 
            TEMPORARY, // t4 
            TEMPORARY, // t5 
            TEMPORARY, // t6 
            "program counter", // pc
        )
    }
}
