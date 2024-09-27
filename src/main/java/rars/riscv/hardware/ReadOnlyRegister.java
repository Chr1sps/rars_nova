package rars.riscv.hardware;

/**
 * <p>ReadOnlyRegister class.</p>
 *
 */
public class ReadOnlyRegister extends Register {
    /**
     * <p>Constructor for ReadOnlyRegister.</p>
     *
     * @param name a {@link java.lang.String} object
     * @param num  a int
     * @param val  a int
     */
    public ReadOnlyRegister(final String name, final int num, final int val) {
        super(name, num, val); // reset value does not matter
    }
}
