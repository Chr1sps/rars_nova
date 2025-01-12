package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.util.Utils;

public final class FNMSUBD extends FusedDouble {
    public static final @NotNull FNMSUBD INSTANCE = new FNMSUBD();

    private FNMSUBD() {
        super("fnmsub.d f1, f2, f3, f4", "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1", "10");
    }

    @Override
    public @NotNull Float64 compute(
        final @NotNull Float64 f1,
        final @NotNull Float64 f2,
        final @NotNull Float64 f3,
        final @NotNull Environment e
    ) {
        Utils.flipRounding(e);
        return Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), e).negate();
    }
}
