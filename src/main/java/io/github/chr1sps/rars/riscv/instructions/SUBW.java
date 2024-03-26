package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>SUBW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SUBW extends ArithmeticW {
    /**
     * <p>Constructor for SUBW.</p>
     */
    public SUBW() {
        super("subw t1,t2,t3", "Subtraction: set t1 to (t2 minus t3) using only the lower 32 bits",
                "0100000", "000", new SUB());
    }
}
