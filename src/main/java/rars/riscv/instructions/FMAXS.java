package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float32;

public final class FMAXS extends Floating {
    public static final @NotNull FMAXS INSTANCE = new FMAXS();

    private FMAXS() {
        super("fmax.s", "Floating MAXimum: assigns f1 to the larger of f1 and f3", "0010100", "001");
    }

    @Override
    public @NotNull Float32 compute(
        final @NotNull Float32 f1,
        final @NotNull Float32 f2,
        final @NotNull Environment env
    ) {
        return Comparisons.maximumNumber(f1, f2, env);
    }
}
