package rars.venus.registers;

import org.jetbrains.annotations.NotNull;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.settings.BoolSetting;
import rars.venus.NumberDisplayBaseChooser;

import static rars.Globals.BOOL_SETTINGS;

public final class FloatingPointWindow extends RegisterBlockWindowBase {
    /**
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

    public FloatingPointWindow() {
        super(FloatingPointRegisterFile.getRegisters(), regToolTips, "32-bit single precision IEEE 754 floating point");
    }

    @Override
    protected @NotNull String formatRegisterValue(final long value, final int base) {
        if (BOOL_SETTINGS.getSetting(BoolSetting.RV64_ENABLED)) {
            return NumberDisplayBaseChooser.formatDoubleNumber(value, base);
        } else {
            return NumberDisplayBaseChooser.formatFloatNumber((int) value, base);
        }
    }

    @Override
    protected void beginObserving() {
        FloatingPointRegisterFile.addRegistersSubscriber(this.processRegisterNotice);
    }

    @Override
    protected void endObserving() {
        FloatingPointRegisterFile.deleteRegistersObserver(this.processRegisterNotice);
    }

    @Override
    protected void resetRegisters() {
        FloatingPointRegisterFile.resetRegisters();
    }
}
