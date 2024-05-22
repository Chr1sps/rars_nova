package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

/**
 * <p>FMIND class.</p>
 *
 */
public class FMIND extends Double {
    /**
     * <p>Constructor for FMIND.</p>
     */
    public FMIND() {
        super("fmin.d", "Floating MINimum (64 bit): assigns f1 to the smaller of f1 and f3", "0010101", "000");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(Float64 f1, Float64 f2, Environment env) {
        return io.github.chr1sps.jsoftfloat.operations.Comparisons.minimumNumber(f1, f2, env);
    }
}
