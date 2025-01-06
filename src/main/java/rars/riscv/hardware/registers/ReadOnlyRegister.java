package rars.riscv.hardware.registers;

import org.jetbrains.annotations.NotNull;

public final class ReadOnlyRegister extends Register {
    public ReadOnlyRegister(final @NotNull String name, final int num, final int val) {
        super(name, num, val); // reset value does not matter
    }
}
