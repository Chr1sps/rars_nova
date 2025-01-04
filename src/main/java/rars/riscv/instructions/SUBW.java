package rars.riscv.instructions;

/**
 * <p>SUBW class.</p>
 */
public final class SUBW extends ArithmeticW {
    public static final SUBW INSTANCE = new SUBW();

    /**
     * <p>Constructor for SUBW.</p>
     */
    private SUBW() {
        super(
            "subw t1,t2,t3", "Subtraction: set t1 to (t2 minus t3) using only the lower 32 bits",
            "0100000", "000", SUB.INSTANCE
        );
    }
}
