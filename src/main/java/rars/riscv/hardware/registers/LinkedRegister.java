package rars.riscv.hardware.registers;

import org.jetbrains.annotations.NotNull;

/**
 * A register which aliases a subset of another register
 */
public final class LinkedRegister extends Register {
    private final @NotNull Register base;
    private final long mask;
    private final int shift;

    public LinkedRegister(final @NotNull String name, final int num, final @NotNull Register base, final long mask) {
        super(name, num, 0L); // reset value does not matter
        this.base = base;
        this.mask = mask;
        this.shift = calculateShift(mask);
    }

    private static int calculateShift(long mask) {
        // Find the lowest 1 bit
        int shift = 0;
        while (mask != 0L && (mask & 1L) == 0L) {
            shift++;
            mask >>>= 1L;
        }
        return shift;
    }

    @Override
    public synchronized long getValueNoNotify() {
        return (base.getValueNoNotify() & mask) >>> shift;
    }

    @Override
    public synchronized long setValue(final long val) {
        final long old = base.getValueNoNotify();
        base.setValue(((val << shift) & mask) | (old & ~mask));
        super.setValue(0L); // value doesn't matter just notify
        return (old & mask) >>> shift;
    }

    @Override
    public synchronized void resetValue() {
        base.resetValue(); // not completely correct, but registers are only reset all together, so it
        // doesn't matter that the other subsets are reset too
    }
}
