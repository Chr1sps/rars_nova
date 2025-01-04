package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

/**
 * <p>FMULD class.</p>
 */
public final class FMULD extends Double {
    public static final FMULD INSTANCE = new FMULD();

    /**
     * <p>Constructor for FMULD.</p>
     */
    private FMULD() {
        super("fmul.d", "Floating MULtiply (64 bit): assigns f1 to f2 * f3", "0001001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 compute(
        final @NotNull Float64 f1, final @NotNull Float64 f2,
        final @NotNull Environment e
    ) {
        return Arithmetic.multiplication(f1, f2, e);
    }
}
