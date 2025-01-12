package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;

public final class FSUBD extends Double {
    public static final @NotNull FSUBD INSTANCE = new FSUBD();

    private FSUBD() {
        super("fsub.d", "Floating SUBtract (64 bit): assigns f1 to f2 - f3", "0000101");
    }

    @Override
    public @NotNull Float64 compute(
        final @NotNull Float64 f1, final @NotNull Float64 f2,
        final @NotNull Environment e
    ) {
        return Arithmetic.subtraction(f1, f2, e);
    }
}
