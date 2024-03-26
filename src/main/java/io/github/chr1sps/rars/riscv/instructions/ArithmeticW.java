package io.github.chr1sps.rars.riscv.instructions;

/**
 * <p>Abstract ArithmeticW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public abstract class ArithmeticW extends Arithmetic {
    private final Arithmetic base;

    /**
     * <p>Constructor for ArithmeticW.</p>
     *
     * @param usage       a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct7      a {@link java.lang.String} object
     * @param funct3      a {@link java.lang.String} object
     * @param base        a {@link io.github.chr1sps.rars.riscv.instructions.Arithmetic} object
     */
    public ArithmeticW(String usage, String description, String funct7, String funct3, Arithmetic base) {
        super(usage, description, funct7, funct3, true);
        this.base = base;
    }

    /**
     * {@inheritDoc}
     */
    protected long compute(long value, long value2) {
        return base.computeW((int) value, (int) value2);
    }
}
