package rars.riscv.instructions;

/**
 * <p>SLLW class.</p>
 */
public final class SLLW extends ArithmeticW {
    public static final SLLW INSTANCE = new SLLW();

    /**
     * <p>Constructor for SLLW.</p>
     */
    private SLLW() {
        super(
            "sllw t1,t2,t3",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by value in low-order 5 bits of t3",
            "0000000",
            "001",
            SLL.INSTANCE
        );
    }
}
