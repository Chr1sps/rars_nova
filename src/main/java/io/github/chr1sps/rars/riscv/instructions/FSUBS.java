package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FSUBS class.</p>
 */
public class FSUBS extends Floating {
    /**
     * <p>Constructor for FSUBS.</p>
     */
    public FSUBS() {
        super("fsub.s", "Floating SUBtract: assigns f1 to f2 - f3", "0000100");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull Float32 f1, Float32 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.subtraction(f1, f2, e);
    }
}
