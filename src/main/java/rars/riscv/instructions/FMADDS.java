package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FMADDS class.</p>
 */
public class FMADDS extends FusedFloat {
    public static final FMADDS INSTANCE = new FMADDS();

    /**
     * <p>Constructor for FMADDS.</p>
     */
    private FMADDS() {
        super("fmadd.s f1, f2, f3, f4", "Fused Multiply Add: Assigns f2*f3+f4 to f1", "00");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Float32 f3, final Environment e) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e);
    }
}
