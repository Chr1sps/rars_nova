package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FNMADDS class.</p>
 */
public class FNMADDS extends FusedFloat {
    /**
     * <p>Constructor for FNMADDS.</p>
     */
    public FNMADDS() {
        super("fnmadd.s f1, f2, f3, f4", "Fused Negate Multiply Add: Assigns -(f2*f3+f4) to f1", "11");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Float32 f3, @NotNull final Environment e) {
        // TODO: test if this is the right behaviour
        flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
