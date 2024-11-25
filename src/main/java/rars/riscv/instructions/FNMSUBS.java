package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;
import rars.util.Utils;

/**
 * <p>FNMSUBS class.</p>
 */
public class FNMSUBS extends FusedFloat {
    public static final FNMSUBS INSTANCE = new FNMSUBS();

    /**
     * <p>Constructor for FNMSUBS.</p>
     */
    private FNMSUBS() {
        super("fnmsub.s f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 compute(@NotNull final Float32 f1, final @NotNull Float32 f2, @NotNull final Float32 f3,
                                    @NotNull final Environment e) {
        Utils.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
