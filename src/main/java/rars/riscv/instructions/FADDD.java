package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

/**
 * <p>FADDD class.</p>
 */
public final class FADDD extends Double {
    public static final FADDD INSTANCE = new FADDD();

    /**
     * <p>Constructor for FADDD.</p>
     */
    private FADDD() {
        super("fadd.d", "Floating ADD (64 bit): assigns f1 to f2 + f3", "0000001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 compute(final @NotNull Float64 f1, final @NotNull Float64 f2,
                                    final @NotNull Environment e) {
        return Arithmetic.add(f1, f2, e);
    }
}
