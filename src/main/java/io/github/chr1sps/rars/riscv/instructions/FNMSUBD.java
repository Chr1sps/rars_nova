package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

/**
 * <p>FNMSUBD class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FNMSUBD extends FusedDouble {
    /**
     * <p>Constructor for FNMSUBD.</p>
     */
    public FNMSUBD() {
        super("fnmsub.d f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(Float64 f1, Float64 f2, Float64 f3, Environment e) {
        FusedFloat.flipRounding(e);
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
