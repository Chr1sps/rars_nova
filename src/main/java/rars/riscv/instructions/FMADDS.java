package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

public final class FMADDS extends FusedFloat {
    public static final @NotNull FMADDS INSTANCE = new FMADDS();

    private FMADDS() {
        super("fmadd.s f1, f2, f3, f4", "Fused Multiply Add: Assigns f2*f3+f4 to f1", "00");
    }

    @Override
    public @NotNull Float32 compute(
        @NotNull final Float32 f1, final @NotNull Float32 f2, final @NotNull Float32 f3,
        final @NotNull Environment e
    ) {
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e);
    }
}
