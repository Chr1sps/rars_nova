package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FADDD class.</p>
 */
public class FADDD extends Double {
    /**
     * <p>Constructor for FADDD.</p>
     */
    public FADDD() {
        super("fadd.d", "Floating ADD (64 bit): assigns f1 to f2 + f3", "0000001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Environment e) {
        return Arithmetic.add(f1, f2, e);
    }
}
