package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Abstract ArithmeticW class.</p>
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
     * @param base        a {@link Arithmetic} object
     */
    public ArithmeticW(@NotNull final String usage, final String description, final String funct7, final String funct3, final Arithmetic base) {
        super(usage, description, funct7, funct3, true);
        this.base = base;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long compute(final long value, final long value2) {
        return base.computeW((int) value, (int) value2);
    }
}
