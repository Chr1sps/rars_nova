package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>DIVUW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class DIVUW extends ArithmeticW {
    /**
     * <p>Constructor for DIVUW.</p>
     */
    public DIVUW() {
        super("divuw t1,t2,t3", "Division: set t1 to the result of t2/t3 using unsigned division limited to 32 bits",
                "0000001", "101", new DIVU());
    }

}
