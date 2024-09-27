package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FNMSUBS class.</p>
 */
public class FNMSUBS extends FusedFloat {
    /**
     * <p>Constructor for FNMSUBS.</p>
     */
    public FNMSUBS() {
        super("fnmsub.s f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, @NotNull final Float32 f3, @NotNull final Environment e) {
        flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
