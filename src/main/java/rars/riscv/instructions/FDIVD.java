package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

/**
 * <p>FDIVD class.</p>
 */
public final class FDIVD extends Double {
    public static final FDIVD INSTANCE = new FDIVD();

    /**
     * <p>Constructor for FDIVD.</p>
     */
    private FDIVD() {
        super("fdiv.d", "Floating DIVide (64 bit): assigns f1 to f2 / f3", "0001101");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 compute(final @NotNull Float64 f1, final @NotNull Float64 f2,
                                    final @NotNull Environment e) {
        return Arithmetic.division(f1, f2, e);
    }
}
