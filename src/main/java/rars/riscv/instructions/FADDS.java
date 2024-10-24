package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

/**
 * <p>FADDS class.</p>
 */
public final class FADDS extends Floating {
    public static final FADDS INSTANCE = new FADDS();

    /**
     * <p>Constructor for FADDS.</p>
     */
    private FADDS() {
        super("fadd.s", "Floating ADD: assigns f1 to f2 + f3", "0000000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float32 compute(@NotNull final Float32 f1, final Float32 f2, final Environment e) {
        return Arithmetic.add(f1, f2, e);
    }
}
