package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>ADDW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class ADDW extends ArithmeticW {
    /**
     * <p>Constructor for ADDW.</p>
     */
    public ADDW() {
        super("addw t1,t2,t3", "Addition: set t1 to (t2 plus t3) using only the lower 32 bits",
                "0000000", "000", new ADD());
    }
}
