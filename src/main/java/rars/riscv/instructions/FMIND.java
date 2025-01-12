package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float64;

public final class FMIND extends Double {
    public static final @NotNull FMIND INSTANCE = new FMIND();

    private FMIND() {
        super("fmin.d", "Floating MINimum (64 bit): assigns f1 to the smaller of f1 and f3", "0010101", "000");
    }

    @Override
    public @NotNull Float64 compute(
        final @NotNull Float64 f1, final @NotNull Float64 f2,
        final @NotNull Environment env
    ) {
        return Comparisons.minimumNumber(f1, f2, env);
    }
}
