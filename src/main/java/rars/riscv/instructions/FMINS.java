package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FMINS class.</p>
 */
public final class FMINS extends Floating {
    public static final FMINS INSTANCE = new FMINS();

    /**
     * <p>Constructor for FMINS.</p>
     */
    private FMINS() {
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
