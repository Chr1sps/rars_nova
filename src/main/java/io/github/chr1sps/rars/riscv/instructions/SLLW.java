package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>SLLW class.</p>
 *
 */
public class SLLW extends ArithmeticW {
    /**
     * <p>Constructor for SLLW.</p>
     */
    public SLLW() {
        super("sllw t1,t2,t3",
                "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by value in low-order 5 bits of t3",
                "0000000", "001", new SLL());
    }
}
