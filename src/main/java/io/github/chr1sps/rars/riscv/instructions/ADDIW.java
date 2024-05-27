package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>ADDIW class.</p>
 *
 */
public class ADDIW extends ImmediateInstruction {
    /**
     * <p>Constructor for ADDIW.</p>
     */
    public ADDIW() {
        super("addiw t1,t2,-100",
                "Addition immediate: set t1 to (t2 plus signed 12-bit immediate) using only the lower 32 bits", "000",
                true);
    }

    /**
     * {@inheritDoc}
     */
    public long compute(long value, long immediate) {
        return (int) value + (int) immediate;
    }
}