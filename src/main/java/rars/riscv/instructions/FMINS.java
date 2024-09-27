package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Comparisons;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMINS class.</p>
 */
public class FMINS extends Floating {
    /**
     * <p>Constructor for FMINS.</p>
     */
    public FMINS() {
        super("fmin.s", "Floating MINimum: assigns f1 to the smaller of f1 and f3", "0010100", "000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 compute(final Float32 f1, final Float32 f2, final Environment env) {
        return Comparisons.minimumNumber(f1, f2, env);
    }
}
