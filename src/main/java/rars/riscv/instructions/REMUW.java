package rars.riscv.instructions;

/**
 * <p>REMUW class.</p>
 */
public final class REMUW extends ArithmeticW {
    public static final REMUW INSTANCE = new REMUW();

    /**
     * <p>Constructor for REMUW.</p>
     */
    private REMUW() {
        super(
            "remuw t1,t2,t3",
            "Remainder: set t1 to the remainder of t2/t3 using unsigned division limited to 32 bits",
            "0000001", "111", REMU.INSTANCE
        );
    }
}
