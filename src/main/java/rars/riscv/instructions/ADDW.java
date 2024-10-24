package rars.riscv.instructions;

/**
 * <p>ADDW class.</p>
 */
public final class ADDW extends ArithmeticW {
    public static final ADDW INSTANCE = new ADDW();

    /**
     * <p>Constructor for ADDW.</p>
     */
    private ADDW() {
        super("addw t1,t2,t3", "Addition: set t1 to (t2 plus t3) using only the lower 32 bits",
                "0000000", "000", ADD.INSTANCE);
    }
}
