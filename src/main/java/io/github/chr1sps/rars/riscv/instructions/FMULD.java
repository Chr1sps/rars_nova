package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMULD class.</p>
 */
public class FMULD extends Double {
    /**
     * <p>Constructor for FMULD.</p>
     */
    public FMULD() {
        super("fmul.d", "Floating MULtiply (64 bit): assigns f1 to f2 * f3", "0001001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull Float64 f1, Float64 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.multiplication(f1, f2, e);
    }
}
