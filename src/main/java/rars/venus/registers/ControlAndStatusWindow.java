package rars.venus.registers;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.settings.BoolSetting;
import rars.venus.NumberDisplayBaseChooser;
import rars.venus.VenusUI;

import static rars.Globals.BOOL_SETTINGS;

public final class ControlAndStatusWindow extends RegisterBlockWindowBase {
    /**
     * The tips to show when hovering over the names of the registers
     */
    // TODO: Maintain order if any new CSRs are added
    private static final String[] regToolTips = {
        /* ustatus" */ "Interrupt status information (set the lowest bit to enable exceptions)",
        /* fflags */ "The accumulated floating point flags",
        /* frm */ "Rounding mode for floating point operatations",
        /* fcsr */ "Both frm and fflags",
        /* uie */ "Finer control for which interrupts are enabled",
        /* utvec */ "The base address of the interrupt handler",
        /* uscratch */ "Scratch for processing inside the interrupt handler",
        /* uepc */ "PC at the time the interrupt was triggered",
        /* ucause */ "Cause of the interrupt (top bit is interrupt vs trap)",
        /* utval */ "Value associated with the cause",
        /* uip */ "Shows if any interrupt is pending and what type",
        /* cycle */ "Number of clock cycles executed",
        /* time */ "Time since some time in the past (Milliseconds since 1/1/1970 in RARS)",
        /* instret */ "Instructions retired (same as cycle in RARS)",
        /* cycleh */ "High 32 bits of cycle",
        /* timeh */ "High 32 bits of time",
        /* instreth */ "High 32 bits of instret"
    };

    public ControlAndStatusWindow(final @NotNull VenusUI mainUI) {
        super(Globals.CS_REGISTER_FILE, regToolTips, "Current 32 bit value", mainUI);
    }

    @Override
    protected @NotNull String formatRegisterValue(final long value, final int base) {
        if (BOOL_SETTINGS.getSetting(BoolSetting.RV64_ENABLED)) {
            return NumberDisplayBaseChooser.formatNumber(value, base);
        } else {
            return NumberDisplayBaseChooser.formatNumber((int) value, base);
        }
    }
}
