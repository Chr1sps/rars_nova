package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

/**
 * <p>FMADDD class.</p>
 *
 */
public class FMADDD extends FusedDouble {
    /**
     * <p>Constructor for FMADDD.</p>
     */
    public FMADDD() {
        super("fmadd.d f1, f2, f3, f4", "Fused Multiply Add (64 bit): Assigns f2*f3+f4 to f1", "00");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(Float64 f1, Float64 f2, Float64 f3, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3, e);
    }
}
