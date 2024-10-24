package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float64;

/**
 * <p>FMAXD class.</p>
 */
public final class FMAXD extends Double {
    public static final FMAXD INSTANCE = new FMAXD();

    /**
     * <p>Constructor for FMAXD.</p>
     */
    private FMAXD() {
        super("fmax.d", "Floating MAXimum (64 bit): assigns f1 to the larger of f1 and f3", "0010101", "001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(final Float64 f1, final Float64 f2, final Environment env) {
        return Comparisons.maximumNumber(f1, f2, env);
    }
}
