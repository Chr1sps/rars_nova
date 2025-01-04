package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.util.ConversionUtils;

public final class ADDIW extends ImmediateInstruction {
    public static final @NotNull ADDIW INSTANCE = new ADDIW();

    private ADDIW() {
        super(
            "addiw t1,t2,-100",
            "Addition immediate: set t1 to (t2 plus signed 12-bit immediate) using only the lower 32 bits",
            "000",
            true
        );
    }

    @Override
    public long compute(final long value, final long immediate) {
        final var result = ConversionUtils.longLowerHalfToInt(value) + ConversionUtils.longLowerHalfToInt(immediate);
        return Integer.valueOf(result).longValue();
    }
}
