package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FMULS class.</p>
 */
public final class FMULS extends Floating {
    public static final FMULS INSTANCE = new FMULS();

    /**
     * <p>Constructor for FMULS.</p>
     */
    private FMULS() {
        super("fmul.s", "Floating MULtiply: assigns f1 to f2 * f3", "0001000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 compute(
        @NotNull final Float32 f1,
        final @NotNull Float32 f2,
        final @NotNull Environment e
    ) {
        return Arithmetic.multiplication(f1, f2, e);
    }
}
