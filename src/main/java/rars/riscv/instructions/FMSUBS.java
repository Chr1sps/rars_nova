package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMSUBS class.</p>
 */
public class FMSUBS extends FusedFloat {
    /**
     * <p>Constructor for FMSUBS.</p>
     */
    public FMSUBS() {
        super("fmsub.s f1, f2, f3, f4", "Fused Multiply Subatract: Assigns f2*f3-f4 to f1", "01");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, @NotNull final Float32 f3, final Environment e) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
