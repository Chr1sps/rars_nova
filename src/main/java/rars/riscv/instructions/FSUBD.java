package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FSUBD class.</p>
 */
public class FSUBD extends Double {
    /**
     * <p>Constructor for FSUBD.</p>
     */
    public FSUBD() {
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
