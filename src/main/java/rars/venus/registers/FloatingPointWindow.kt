package rars.venus.registers

import rars.riscv.hardware.registerfiles.FloatingPointRegisterFile
import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.venus.NumberDisplayBaseChooser
import rars.venus.VenusUI

class FloatingPointWindow(
    fpRegisterFile: FloatingPointRegisterFile,
    mainUI: VenusUI,
    settings: AllSettings,
) : RegisterBlockWindowBase(
    fpRegisterFile,
    regToolTips,
    "32-bit single precision IEEE 754 floating point",
    mainUI,
    settings
) {
    override fun formatRegisterValue(value: Long, base: Int): String =
        if (settings.boolSettings.getSetting(BoolSetting.RV64_ENABLED)) {
            NumberDisplayBaseChooser.formatDoubleNumber(value, base)
        } else {
            NumberDisplayBaseChooser.formatFloatNumber(value.toInt(), base)
        }

    companion object {
        /** The tips to show when hovering over the names of the registers */
        private val regToolTips = arrayOf(
            "floating point temporary", // ft0
            "floating point temporary", // ft1
            "floating point temporary", // ft2
            "floating point temporary", // ft3
            "floating point temporary", // ft4
            "floating point temporary", // ft5
            "floating point temporary", // ft6
            "floating point temporary", // ft7
            "saved temporary (preserved across call)", // fs0
            "saved temporary (preserved across call)", // fs1
            "floating point argument / return value", // fa0
            "floating point argument / return value", // fa1
            "floating point argument", // fa2
            "floating point argument", // fa3
            "floating point argument", // fa4
            "floating point argument", // fa5
            "floating point argument", // fa6
            "floating point argument", // fa7
            "saved temporary (preserved across call)", // fs2
            "saved temporary (preserved across call)", // fs3
            "saved temporary (preserved across call)", // fs4
            "saved temporary (preserved across call)", // fs5
            "saved temporary (preserved across call)", // fs6
            "saved temporary (preserved across call)", // fs7
            "saved temporary (preserved across call)" // fs8
        )
    }
}
