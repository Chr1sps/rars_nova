package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
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
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, @NotNull final Float64 f3, final Environment e) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
