package rars.riscv.instructions;

/**
 * <p>SRAW class.</p>
 */
public final class SRAW extends ArithmeticW {
    public static final SRAW INSTANCE = new SRAW();

    /**
     * <p>Constructor for SRAW.</p>
     */
    public SRAW() {
        super("sraw t1,t2,t3",
                "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by second in low-order 5 bits of t3",
                "0100000", "101", SRA.INSTANCE);
    }
}
