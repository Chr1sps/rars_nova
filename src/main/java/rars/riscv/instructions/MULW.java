package rars.riscv.instructions;

/**
 * <p>MULW class.</p>
 */
public final class MULW extends ArithmeticW {
    public static final MULW INSTANCE = new MULW();

    /**
     * <p>Constructor for MULW.</p>
     */
    private MULW() {
        super(
            "mulw t1,t2,t3",
            "Multiplication: set t1 to the lower 32 bits of t2*t3 using only the lower 32 bits of the input",
            "0000001", "000", MUL.INSTANCE
        );
    }
}
