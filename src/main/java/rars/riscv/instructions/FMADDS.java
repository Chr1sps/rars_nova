package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMADDS class.</p>
 */
public class FMADDS extends FusedFloat {
    /**
     * <p>Constructor for FMADDS.</p>
     */
    public FMADDS() {
        super("fmadd.s f1, f2, f3, f4", "Fused Multiply Add: Assigns f2*f3+f4 to f1", "00");
    }

    /**
     * {@inheritDoc}
     */
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Float32 f3, final Environment e) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e);
    }
}
