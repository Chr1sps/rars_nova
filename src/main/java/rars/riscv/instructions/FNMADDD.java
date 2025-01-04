package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.util.Utils;

/**
 * <p>FNMADDD class.</p>
 */
public final class FNMADDD extends FusedDouble {
    public static final FNMADDD INSTANCE = new FNMADDD();

    /**
     * <p>Constructor for FNMADDD.</p>
     */
    private FNMADDD() {
        super("fnmadd.d f1, f2, f3, f4", "Fused Negate Multiply Add (64 bit): Assigns -(f2*f3+f4) to f1", "11");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 compute(
        @NotNull final Float64 f1, final Float64 f2, final Float64 f3,
        @NotNull final Environment e
    ) {
        // TODO: test if this is the right behaviour
        Utils.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
