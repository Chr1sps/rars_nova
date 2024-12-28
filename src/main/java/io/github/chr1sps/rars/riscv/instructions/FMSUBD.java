package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMSUBD class.</p>
 */
public class FMSUBD extends FusedDouble {
    /**
     * <p>Constructor for FMSUBD.</p>
     */
    public FMSUBD() {
        super("fmsub.d f1, f2, f3, f4", "Fused Multiply Subatract: Assigns f2*f3-f4 to f1", "01");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(@NotNull Float64 f1, Float64 f2, @NotNull Float64 f3, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
