package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;
import rars.util.Utils;

public final class FNMADDS extends FusedFloat {
    public static final @NotNull FNMADDS INSTANCE = new FNMADDS();

    private FNMADDS() {
        super("fnmadd.s f1, f2, f3, f4", "Fused Negate Multiply Add: Assigns -(f2*f3+f4) to f1", "11");
    }

    @Override
    public @NotNull Float32 compute(
        @NotNull final Float32 f1, final @NotNull Float32 f2, final @NotNull Float32 f3,
        @NotNull final Environment e
    ) {
        // TODO: test if this is the right behaviour
        Utils.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3, e).negate();
    }
}
