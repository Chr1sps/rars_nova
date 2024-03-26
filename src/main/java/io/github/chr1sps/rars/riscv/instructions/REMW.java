package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>REMW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class REMW extends ArithmeticW {
    /**
     * <p>Constructor for REMW.</p>
     */
    public REMW() {
        super("remw t1,t2,t3", "Remainder: set t1 to the remainder of t2/t3 using only the lower 32 bits",
                "0000001", "110", new REM());
    }
}
