package rars.venus.registers

import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.venus.NumberDisplayBaseChooser
import rars.venus.VenusUI

class ControlAndStatusWindow(
    csRegisterFile: CSRegisterFile,
    mainUI: VenusUI,
    settings: AllSettings,
) : RegisterBlockWindowBase(
    csRegisterFile,
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
        // Maintain order if any new CSRs are added
        /** The tips to show when hovering over the names of the registers */
        private val regToolTips = arrayOf(
            "Interrupt status information (set the lowest bit to enable exceptions)", // ustatus
            "The accumulated floating point flags", // fflags
            "Rounding mode for floating point operatations", // frm
            "Both frm and fflags", // fcsr
            "Finer control for which interrupts are enabled", // uie
            "The base address of the interrupt handler", // utvec
            "Scratch for processing inside the interrupt handler", // uscratch
            "PC at the time the interrupt was triggered", // uepc
            "Cause of the interrupt (top bit is interrupt vs trap)", // ucause
            "Value associated with the cause", // utval
            "Shows if any interrupt is pending and what type", // uip
            "Number of clock cycles executed", // cycle
            "Time since some time in the past (Milliseconds since 1/1/1970 in RARS)", // time
            "Instructions retired (same as cycle in RARS)", // instret
            "High 32 bits of cycle", // cycleh
            "High 32 bits of time", // timeh
            "High 32 bits of instret" // instreth
        )
    }
}
