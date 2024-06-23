package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
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
    public Float64 compute(@NotNull Float64 f1, Float64 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.subtraction(f1, f2, e);
    }
}
