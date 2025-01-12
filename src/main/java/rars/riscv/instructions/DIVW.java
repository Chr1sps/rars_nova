package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;

public final class DIVW extends ArithmeticW {
    public static final @NotNull DIVW INSTANCE = new DIVW();

    private DIVW() {
        super(
            "divw t1,t2,t3", "Division: set t1 to the result of t2/t3 using only the lower 32 bits",
            "0000001", "100", DIV.INSTANCE
        );
    }
}
