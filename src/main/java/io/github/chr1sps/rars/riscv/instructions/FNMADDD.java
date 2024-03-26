package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

/**
 * <p>FNMADDD class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FNMADDD extends FusedDouble {
    /**
     * <p>Constructor for FNMADDD.</p>
     */
    public FNMADDD() {
        super("fnmadd.d f1, f2, f3, f4", "Fused Negate Multiply Add (64 bit): Assigns -(f2*f3+f4) to f1", "11");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(Float64 f1, Float64 f2, Float64 f3, Environment e) {
        // TODO: test if this is the right behaviour
        FusedFloat.flipRounding(e);
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
