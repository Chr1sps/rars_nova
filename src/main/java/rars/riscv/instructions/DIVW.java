package rars.riscv.instructions;

/**
 * <p>DIVW class.</p>
 */
public final class DIVW extends ArithmeticW {
    public static final DIVW INSTANCE = new DIVW();

    /**
     * <p>Constructor for DIVW.</p>
     */
    private DIVW() {
        super(
            "divw t1,t2,t3", "Division: set t1 to the result of t2/t3 using only the lower 32 bits",
            "0000001", "100", DIV.INSTANCE
        );
    }
}
