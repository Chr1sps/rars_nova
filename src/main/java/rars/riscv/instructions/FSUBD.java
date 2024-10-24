package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

/**
 * <p>FSUBD class.</p>
 */
public final class FSUBD extends Double {
    public static final FSUBD INSTANCE = new FSUBD();

    /**
     * <p>Constructor for FSUBD.</p>
     */
    private FSUBD() {
        super("fsub.d", "Floating SUBtract (64 bit): assigns f1 to f2 - f3", "0000101");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull final Float64 f1, final Float64 f2, final Environment e) {
        return Arithmetic.subtraction(f1, f2, e);
    }
}
