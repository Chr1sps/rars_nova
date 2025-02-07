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
        /** The tips to show when hovering over the names of the registers */
        private val regToolTips = arrayOf(
            "constant 0", // zero 
            "return address (used by function call)", // ra 
            "stack pointer", // sp 
            "pointer to global area", // gp 
            "pointer to thread local data (not given a value)", // tp 
            "temporary (not preserved across call)", // t0 
            "temporary (not preserved across call)", // t1 
            "temporary (not preserved across call)", // t2 
            "saved temporary (preserved across call)", // s0 
            "saved temporary (preserved across call)", // s1 
            "argument 1 / return 1", // a0 
            "argument 2 / return 2", // a1 
            "argument 3", // a2 
            "argument 4", // a3 
            "argument 5", // a4 
            "argument 6", // a5 
            "argument 7", // a6 
            "argument 8", // a7 
            "saved temporary (preserved across call)", // s2 
            "saved temporary (preserved across call)", // s3 
            "saved temporary (preserved across call)", // s4 
            "saved temporary (preserved across call)", // s5 
            "saved temporary (preserved across call)", // s6 
            "saved temporary (preserved across call)", // s7 
            "saved temporary (preserved across call)", // s8 
            "saved temporary (preserved across call)", // s9 
            "saved temporary (preserved across call)", // s10 
            "saved temporary (preserved across call)", // s11 
            "temporary (not preserved across call)", // t3 
            "temporary (not preserved across call)", // t4 
            "temporary (not preserved across call)", // t5 
            "temporary (not preserved across call)", // t6 
            "program counter", // pc
        )
    }
}
