package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FNMSUBD class.</p>
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
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, @NotNull final Float64 f3, @NotNull final Environment e) {
        FusedFloat.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
