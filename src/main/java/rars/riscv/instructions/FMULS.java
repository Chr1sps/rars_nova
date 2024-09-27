package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMULS class.</p>
 */
public class FMULS extends Floating {
    /**
     * <p>Constructor for FMULS.</p>
     */
    public FMULS() {
        super("fmul.s", "Floating MULtiply: assigns f1 to f2 * f3", "0001000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Environment e) {
        return Arithmetic.multiplication(f1, f2, e);
    }
}
