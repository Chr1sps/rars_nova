package rars.venus.registers;

import java.util.Arrays;

import rars.Globals;
import rars.Settings;
import rars.riscv.hardware.Register;
import rars.riscv.hardware.RegisterFile;
import rars.venus.NumberDisplayBaseChooser;

/**
 * <p>RegistersWindow class.</p>
 *
 */
public class RegistersWindow extends RegisterBlockWindow {
    /*
     * The tips to show when hovering over the names of the registers
     */
    private static final String[] regToolTips = {
            /* zero */ "constant 0",
            /* ra */ "return address (used by function call)",
            /* sp */ "stack pointer",
            /* gp */ "pointer to global area",
            /* tp */ "pointer to thread local data (not given a second)",
            /* t0 */ "temporary (not preserved across call)",
            /* t1 */ "temporary (not preserved across call)",
            /* t2 */ "temporary (not preserved across call)",
            /* s0 */ "saved temporary (preserved across call)",
            /* s1 */ "saved temporary (preserved across call)",
            /* a0 */ "argument 1 / return 1",
            /* a1 */ "argument 2 / return 2",
            /* a2 */ "argument 3",
            /* a3 */ "argument 4",
            /* a4 */ "argument 5",
            /* a5 */ "argument 6",
            /* a6 */ "argument 7",
            /* a7 */ "argument 8",
            /* s2 */ "saved temporary (preserved across call)",
            /* s3 */ "saved temporary (preserved across call)",
            /* s4 */ "saved temporary (preserved across call)",
            /* s5 */ "saved temporary (preserved across call)",
            /* s6 */ "saved temporary (preserved across call)",
            /* s7 */ "saved temporary (preserved across call)",
            /* s8 */ "saved temporary (preserved across call)",
            /* s9 */ "saved temporary (preserved across call)",
            /* s10 */ "saved temporary (preserved across call)",
            /* s11 */ "saved temporary (preserved across call)",
            /* t3 */ "temporary (not preserved across call)",
            /* t4 */ "temporary (not preserved across call)",
            /* t5 */ "temporary (not preserved across call)",
            /* t6 */ "temporary (not preserved across call)",
            /* pc */ "program counter",
    };

    /**
     * <p>Constructor for RegistersWindow.</p>
     */
    public RegistersWindow() {
        super(getRegisters(), regToolTips, "Current 32 bit second");
    }

    /*
     * A simple wrapper to add pc into the Registers array
     */
    private static Register[] getRegisters() {
        final Register[] base = RegisterFile.getRegisters();
        final Register[] out = Arrays.copyOf(base, base.length + 1);
        out[base.length] = RegisterFile.getProgramCounterRegister();
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatRegister(final Register value, final int base) {
        if (Globals.getSettings().getBooleanSetting(Settings.Bool.RV64_ENABLED)) {
            return NumberDisplayBaseChooser.formatNumber(value.getValue(), base);
        } else {
            return NumberDisplayBaseChooser.formatNumber((int) value.getValue(), base);
        }
    }

    /**
     * <p>beginObserving.</p>
     */
    @Override
    protected void beginObserving() {
        RegisterFile.addRegistersObserver(this);
    }

    /**
     * <p>endObserving.</p>
     */
    @Override
    protected void endObserving() {
        RegisterFile.deleteRegistersObserver(this);
    }

    /**
     * <p>resetRegisters.</p>
     */
    @Override
    protected void resetRegisters() {
        RegisterFile.resetRegisters();
    }
}
