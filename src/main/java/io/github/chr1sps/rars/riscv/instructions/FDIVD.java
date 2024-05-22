package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

/**
 * <p>FDIVD class.</p>
 *
 */
public class FDIVD extends Double {
    /**
     * <p>Constructor for FDIVD.</p>
     */
    public FDIVD() {
        super("fdiv.d", "Floating DIVide (64 bit): assigns f1 to f2 / f3", "0001101");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(Float64 f1, Float64 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.division(f1, f2, e);
    }
}
