package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMULS class.</p>
 */
public class FMULS extends Floating {
    /**
     * <p>Constructor for FMULS.</p>
     */
    public FMULS() {
        super("fmul.s", "Floating MULtiply: assigns f1 to f2 * f3", "0001000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull Float32 f1, Float32 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.multiplication(f1, f2, e);
    }
}
