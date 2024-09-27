package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FDIVD class.</p>
 */
public class FDIVD extends Double {
    /**
     * <p>Constructor for FDIVD.</p>
     */
    public FDIVD() {
        super("fdiv.d", "Floating DIVide (64 bit): assigns f1 to f2 / f3", "0001101");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Environment e) {
        return Arithmetic.division(f1, f2, e);
    }
}
