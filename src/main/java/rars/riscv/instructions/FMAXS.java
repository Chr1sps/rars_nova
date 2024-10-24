package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FMAXS class.</p>
 */
public final class FMAXS extends Floating {
    public static final FMAXS INSTANCE = new FMAXS();

    /**
     * <p>Constructor for FMAXS.</p>
     */
    private FMAXS() {
        super("fmax.s", "Floating MAXimum: assigns f1 to the larger of f1 and f3", "0010100", "001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(final Float32 f1, final Float32 f2, final Environment env) {
        return Comparisons.maximumNumber(f1, f2, env);
    }
}
