package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.operations.Arithmetic;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FADDS class.</p>
 */
public class FADDS extends Floating {
    /**
     * <p>Constructor for FADDS.</p>
     */
    public FADDS() {
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
