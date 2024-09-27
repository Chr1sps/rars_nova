package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMULD class.</p>
 */
public class FMULD extends Double {
    /**
     * <p>Constructor for FMULD.</p>
     */
    public FMULD() {
        super("fmul.d", "Floating MULtiply (64 bit): assigns f1 to f2 * f3", "0001001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Environment e) {
        return Arithmetic.multiplication(f1, f2, e);
    }
}
