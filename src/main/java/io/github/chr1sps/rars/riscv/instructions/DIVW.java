package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>DIVW class.</p>
 *
 */
public class DIVW extends ArithmeticW {
    /**
     * <p>Constructor for DIVW.</p>
     */
    public DIVW() {
        super("divw t1,t2,t3", "Division: set t1 to the result of t2/t3 using only the lower 32 bits",
                "0000001", "100", new DIV());
    }
}
