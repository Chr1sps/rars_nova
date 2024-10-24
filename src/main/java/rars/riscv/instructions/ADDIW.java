package rars.riscv.instructions;

/**
 * <p>ADDIW class.</p>
 */
public final class ADDIW extends ImmediateInstruction {
    public static final ImmediateInstruction INSTANCE = new ADDIW();

    /**
     * <p>Constructor for ADDIW.</p>
     */
    private ADDIW() {
        super("addiw t1,t2,-100",
                "Addition immediate: set t1 to (t2 plus signed 12-bit immediate) using only the lower 32 bits", "000");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long compute(final long value, final long immediate) {
        return (int) value + (int) immediate;
    }
}
