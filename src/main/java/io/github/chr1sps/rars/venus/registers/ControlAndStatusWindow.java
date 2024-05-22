package io.github.chr1sps.rars.venus.registers;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.riscv.hardware.ControlAndStatusRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.Register;
import io.github.chr1sps.rars.venus.NumberDisplayBaseChooser;

/**
 * <p>ControlAndStatusWindow class.</p>
 *
 */
public class ControlAndStatusWindow extends RegisterBlockWindow {
    /*
     * The tips to show when hovering over the names of the registers
     * TODO: Maintain order if any new CSRs are added
     */
    private static final String[] regToolTips = {
            /* ustatus" */ "Interrupt status information (set the lowest bit to enable exceptions)",
            /* fflags */ "The accumulated floating point flags",
            /* frm */ "Rounding mode for floating point operatations",
            /* fcsr */ "Both frm and fflags",
            /* uie */ "Finer control for which interrupts are enabled",
            /* utvec */ "The base address of the interrupt handler",
            /* uscratch */"Scratch for processing inside the interrupt handler",
            /* uepc */ "PC at the time the interrupt was triggered",
            /* ucause */ "Cause of the interrupt (top bit is interrupt vs trap)",
            /* utval */ "Value associated with the cause",
            /* uip */ "Shows if any interrupt is pending and what type",
            /* cycle */ "Number of clock cycles executed",
            /* time */ "Time since some time in the past (Milliseconds since 1/1/1970 in RARS)",
            /* instret */"Instructions retired (same as cycle in RARS)",
            /* cycleh */ "High 32 bits of cycle",
            /* timeh */ "High 32 bits of time",
            /* instreth */ "High 32 bits of instret"
    };

    /**
     * <p>Constructor for ControlAndStatusWindow.</p>
     */
    public ControlAndStatusWindow() {
        super(ControlAndStatusRegisterFile.getRegisters(), regToolTips, "Current 32 bit value");
    }

    /**
     * {@inheritDoc}
     */
    protected String formatRegister(Register value, int base) {
        if (Globals.getSettings().getBooleanSetting(Settings.Bool.RV64_ENABLED)) {
            return NumberDisplayBaseChooser.formatNumber(value.getValue(), base);
        } else {
            return NumberDisplayBaseChooser.formatNumber((int) value.getValue(), base);
        }
    }

    /**
     * <p>beginObserving.</p>
     */
    protected void beginObserving() {
        ControlAndStatusRegisterFile.addRegistersObserver(this);
    }

    /**
     * <p>endObserving.</p>
     */
    protected void endObserving() {
        ControlAndStatusRegisterFile.deleteRegistersObserver(this);
    }

    /**
     * <p>resetRegisters.</p>
     */
    public void resetRegisters() {
        ControlAndStatusRegisterFile.resetRegisters();
    }
}
