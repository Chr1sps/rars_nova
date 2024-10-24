package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FSUBS class.</p>
 */
public final class FSUBS extends Floating {
    public static final FSUBS INSTANCE = new FSUBS();

    /**
     * <p>Constructor for FSUBS.</p>
     */
    private FSUBS() {
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
