package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

public final class FMSUBS extends FusedFloat {
    public static final FMSUBS INSTANCE = new FMSUBS();

    private FMSUBS() {
        super("fmsub.s f1, f2, f3, f4", "Fused Multiply Subatract: Assigns f2*f3-f4 to f1", "01");
    }

    @Override
    public @NotNull Float32 compute(
        @NotNull final Float32 f1, final @NotNull Float32 f2, @NotNull final Float32 f3,
        final @NotNull Environment e
    ) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e);
    }
}
