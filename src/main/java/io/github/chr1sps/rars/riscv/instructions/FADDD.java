package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FADDD class.</p>
 */
public class FADDD extends Double {
    /**
     * <p>Constructor for FADDD.</p>
     */
    public FADDD() {
        super("fadd.d", "Floating ADD (64 bit): assigns f1 to f2 + f3", "0000001");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 compute(@NotNull Float64 f1, Float64 f2, Environment e) {
        return io.github.chr1sps.jsoftfloat.operations.Arithmetic.add(f1, f2, e);
    }
}
