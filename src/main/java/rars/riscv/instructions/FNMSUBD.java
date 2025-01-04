package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.util.Utils;

/**
 * <p>FNMSUBD class.</p>
 */
public final class FNMSUBD extends FusedDouble {
    public static final FNMSUBD INSTANCE = new FNMSUBD();

    /**
     * <p>Constructor for FNMSUBD.</p>
     */
    private FNMSUBD() {
        super("fnmsub.d f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(
        @NotNull final Float64 f1,
        final Float64 f2,
        @NotNull final Float64 f3,
        @NotNull final Environment e
    ) {
        Utils.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
