package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;

/**
 * <p>FNMADDS class.</p>
 *
 */
public class FNMADDS extends FusedFloat {
    /**
     * <p>Constructor for FNMADDS.</p>
     */
    public FNMADDS() {
        super("fnmadd.s f1, f2, f3, f4", "Fused Negate Multiply Add: Assigns -(f2*f3+f4) to f1", "11");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(Float32 f1, Float32 f2, Float32 f3, Environment e) {
        // TODO: test if this is the right behaviour
        flipRounding(e);
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
