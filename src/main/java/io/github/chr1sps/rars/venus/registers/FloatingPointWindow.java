package io.github.chr1sps.rars.venus.registers;

import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.Register;
import io.github.chr1sps.rars.venus.NumberDisplayBaseChooser;

/**
 * <p>FloatingPointWindow class.</p>
 */
public class FloatingPointWindow extends RegisterBlockWindow {
    /*
     * The tips to show when hovering over the names of the registers
     */
    private static final String[] regToolTips = {
            /* ft0 */ "floating point temporary",
            /* ft1 */ "floating point temporary",
            /* ft2 */ "floating point temporary",
            /* ft3 */ "floating point temporary",
            /* ft4 */ "floating point temporary",
            /* ft5 */ "floating point temporary",
            /* ft6 */ "floating point temporary",
            /* ft7 */ "floating point temporary",
            /* fs0 */ "saved temporary (preserved across call)",
            /* fs1 */ "saved temporary (preserved across call)",
            /* fa0 */ "floating point argument / return value",
            /* fa1 */ "floating point argument / return value",
            /* fa2 */ "floating point argument",
            /* fa3 */ "floating point argument",
            /* fa4 */ "floating point argument",
            /* fa5 */ "floating point argument",
            /* fa6 */ "floating point argument",
            /* fa7 */ "floating point argument",
            /* fs2 */ "saved temporary (preserved across call)",
            /* fs3 */ "saved temporary (preserved across call)",
            /* fs4 */ "saved temporary (preserved across call)",
            /* fs5 */ "saved temporary (preserved across call)",
            /* fs6 */ "saved temporary (preserved across call)",
            /* fs7 */ "saved temporary (preserved across call)",
            /* fs8 */ "saved temporary (preserved across call)",
            /* fs9 */ "saved temporary (preserved across call)",
            /* fs10 */ "saved temporary (preserved across call)",
            /* fs11 */ "saved temporary (preserved across call)",
            /* ft8 */ "floating point temporary",
            /* ft9 */ "floating point temporary",
            /* ft10 */ "floating point temporary",
            /* ft11 */ "floating point temporary"
    };

    /**
     * <p>Constructor for FloatingPointWindow.</p>
     */
    public FloatingPointWindow() {
        super(FloatingPointRegisterFile.getRegisters(), regToolTips, "32-bit single precision IEEE 754 floating point");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatRegister(Register value, int base) {
        long val = value.getValue();
        if ((val & 0xFFFFFFFF_00000000L) == 0xFFFFFFFF_00000000L) {
            return NumberDisplayBaseChooser.formatFloatNumber((int) val, base);
        } else {
            return NumberDisplayBaseChooser.formatDoubleNumber(val, base);
        }
    }

    /**
     * <p>beginObserving.</p>
     */
    @Override
    protected void beginObserving() {
        FloatingPointRegisterFile.addRegistersSubscriber(this);
    }

    /**
     * <p>endObserving.</p>
     */
    @Override
    protected void endObserving() {
        FloatingPointRegisterFile.deleteRegistersObserver(this);
    }

    /**
     * <p>resetRegisters.</p>
     */
    @Override
    protected void resetRegisters() {
        FloatingPointRegisterFile.resetRegisters();
    }
}
