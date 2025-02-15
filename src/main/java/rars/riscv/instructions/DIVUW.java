package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;

public final class DIVUW extends ArithmeticW {
    public static final @NotNull DIVUW INSTANCE = new DIVUW();

    private DIVUW() {
        super(
            "divuw t1,t2,t3", "Division: set t1 to the result of t2/t3 using unsigned division limited to 32 bits",
            "0000001", "101", DIVU.INSTANCE
        );
    }

}
