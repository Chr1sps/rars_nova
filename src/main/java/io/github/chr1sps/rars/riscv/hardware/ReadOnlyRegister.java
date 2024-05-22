package io.github.chr1sps.rars.riscv.hardware;

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
    public ReadOnlyRegister(String name, int num, int val) {
        super(name, num, val); // reset value does not matter
    }
}
