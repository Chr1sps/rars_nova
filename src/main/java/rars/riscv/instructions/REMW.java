package rars.riscv.instructions;

/**
 * <p>REMW class.</p>
 */
public final class REMW extends ArithmeticW {
    public static final REMW INSTANCE = new REMW();

    /**
     * <p>Constructor for REMW.</p>
     */
    private REMW() {
        super("remw t1,t2,t3", "Remainder: set t1 to the remainder of t2/t3 using only the lower 32 bits",
                "0000001", "110", REM.INSTANCE);
    }
}
