package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float32;

public final class FSUBS extends Floating {
    public static final @NotNull FSUBS INSTANCE = new FSUBS();

    private FSUBS() {
        super("fsub.s", "Floating SUBtract: assigns f1 to f2 - f3", "0000100");
    }

    @Override
    public @NotNull Float32 compute(
        @NotNull final Float32 f1,
        final @NotNull Float32 f2,
        final @NotNull Environment e
    ) {
        return Arithmetic.subtraction(f1, f2, e);
    }
}
