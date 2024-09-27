package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FNMADDD class.</p>
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
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Float64 f3, @NotNull final Environment e) {
        // TODO: test if this is the right behaviour
        FusedFloat.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
