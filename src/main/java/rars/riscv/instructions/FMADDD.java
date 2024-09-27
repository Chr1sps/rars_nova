package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMADDD class.</p>
 */
public class FMADDD extends FusedDouble {
    /**
     * <p>Constructor for FMADDD.</p>
     */
    public FMADDD() {
        super("fmadd.d f1, f2, f3, f4", "Fused Multiply Add (64 bit): Assigns f2*f3+f4 to f1", "00");
    }

    /**
     * {@inheritDoc}
     */
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Float64 f3, final Environment e) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e);
    }
}
