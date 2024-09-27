package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FSUBS class.</p>
 */
public class FSUBS extends Floating {
    /**
     * <p>Constructor for FSUBS.</p>
     */
    public FSUBS() {
        super("fsub.s", "Floating SUBtract: assigns f1 to f2 - f3", "0000100");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Environment e) {
        return Arithmetic.subtraction(f1, f2, e);
    }
}
