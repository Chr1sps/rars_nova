package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;

/**
 * <p>FMAXS class.</p>
 *
 */
public class FMAXS extends Floating {
    /**
     * <p>Constructor for FMAXS.</p>
     */
    public FMAXS() {
        super("fmax.s", "Floating MAXimum: assigns f1 to the larger of f1 and f3", "0010100", "001");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(Float32 f1, Float32 f2, Environment env) {
        return io.github.chr1sps.jsoftfloat.operations.Comparisons.maximumNumber(f1, f2, env);
    }
}
