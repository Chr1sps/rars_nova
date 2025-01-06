package rars.riscv.hardware.registers;

import org.jetbrains.annotations.NotNull;

/**
 * A register which aliases a subset of another register
 */
public final class MaskedRegister extends Register {
    private final long mask;

    /**
     * <p>Constructor for MaskedRegister.</p>
     *
     * @param name
     *     the name to assign
     * @param num
     *     the number to assign
     * @param val
     *     the reset value
     * @param mask
     *     the bits to use
     */
    public MaskedRegister(final @NotNull String name, final int num, final long val, final long mask) {
        super(name, num, val); // reset value does not matter
        this.mask = mask;
    }

    @Override
    public synchronized long setValue(final long val) {
        final long current = this.getValue();
        super.setValue((current & mask) | (val & ~mask));
        return current;
    }
}
