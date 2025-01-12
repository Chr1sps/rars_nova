package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

public final class FMSUBD extends FusedDouble {
    public static final @NotNull FMSUBD INSTANCE = new FMSUBD();

    private FMSUBD() {
        super("fmsub.d f1, f2, f3, f4", "Fused Multiply Subatract: Assigns f2*f3-f4 to f1", "01");
    }

    @Override
    public @NotNull Float64 compute(
        @NotNull final Float64 f1,
        final @NotNull Float64 f2,
        @NotNull final Float64 f3,
        final @NotNull Environment e
    ) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
