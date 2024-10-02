package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.jsoftfloat.operations.Comparisons;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMIND class.</p>
 */
public class FMIND extends Double {
    /**
     * <p>Constructor for FMIND.</p>
     */
    public FMIND() {
        super("fmin.d", "Floating MINimum (64 bit): assigns f1 to the smaller of f1 and f3", "0010101", "000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 compute(final Float64 f1, final Float64 f2, final Environment env) {
        return Comparisons.minimumNumber(f1, f2, env);
    }
}
