package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;

/**
 * <p>FMINS class.</p>
 *
 */
public class FMINS extends Floating {
    /**
     * <p>Constructor for FMINS.</p>
     */
    public FMINS() {
        super("fmin.s", "Floating MINimum: assigns f1 to the smaller of f1 and f3", "0010100", "000");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(Float32 f1, Float32 f2, Environment env) {
        return io.github.chr1sps.jsoftfloat.operations.Comparisons.minimumNumber(f1, f2, env);
    }
}
