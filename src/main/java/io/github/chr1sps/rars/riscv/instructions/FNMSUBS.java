package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;

/**
 * <p>FNMSUBS class.</p>
 *
 */
public class FNMSUBS extends FusedFloat {
    /**
     * <p>Constructor for FNMSUBS.</p>
     */
    public FNMSUBS() {
        super("fnmsub.s f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(Float32 f1, Float32 f2, Float32 f3, Environment e) {
        flipRounding(e);
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
