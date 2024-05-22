package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;

/**
 * <p>FMSUBS class.</p>
 *
 */
public class FMSUBS extends FusedFloat {
    /**
     * <p>Constructor for FMSUBS.</p>
     */
    public FMSUBS() {
        super("fmsub.s f1, f2, f3, f4", "Fused Multiply Subatract: Assigns f2*f3-f4 to f1", "01");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(Float32 f1, Float32 f2, Float32 f3, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
