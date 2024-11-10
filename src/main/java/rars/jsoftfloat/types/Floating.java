package rars.jsoftfloat.types;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.internal.ExactFloat;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * General classifications that any floating point class needs to provide.
 *
 * @param <T>
 */
public interface Floating<T extends Floating<T>> {
    // TODO: order/group these

    static @NotNull BitSet fromLong(final long l) {
        return BitSet.valueOf(new long[]{l});
    }

    static @NotNull BitSet fromInt(final int i) {
        final var bytes = ByteBuffer.allocate(Integer.BYTES).putInt(i);
        return BitSet.valueOf(bytes);
    }

    static @NotNull BitSet shiftLeft(@NotNull final BitSet bits, final int shift) {
        final var result = new BitSet(bits.length());
        for (int i = 0; i < bits.length(); i++) {
            result.set(i + shift, bits.get(i));
        }
//        for (int i = 0; i < bits.length(); i++) {
//            result.set(i + shift, bits.get(i));
//        }
        return result;
    }

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    boolean isSignMinus();

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    boolean isInfinite();

    /**
     * <p>isFinite.</p>
     *
     * @return a boolean
     */
    default boolean isFinite() {
        return this.isZero() || this.isNormal() || this.isSubnormal();
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    boolean isNormal();

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    boolean isSubnormal();

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    boolean isNaN();

    // TODO: consider making a full bit field representation method for generic
    // conversions

    /**
     * <p>isSignalling.</p>
     *
     * @return a boolean
     */
    boolean isSignalling();

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    boolean isZero();

    /**
     * <p>maxPrecision.</p>
     *
     * @return a int
     */
    int maxPrecision();

    /**
     * <p>NaN.</p>
     *
     * @return a T object
     */
    @NotNull T NaN();

    /**
     * <p>Zero.</p>
     *
     * @return a T object
     */
    @NotNull T Zero();

    /**
     * <p>NegativeZero.</p>
     *
     * @return a T object
     */
    @NotNull T NegativeZero();

    /**
     * <p>Infinity.</p>
     *
     * @return a T object
     */
    @NotNull T Infinity();

    /**
     * <p>NegativeInfinity.</p>
     *
     * @return a T object
     */
    @NotNull T NegativeInfinity();

    /**
     * <p>fromExactFloat.</p>
     *
     * @param f   a {@link ExactFloat} object
     * @param env a {@link Environment} object
     * @return a T object
     */
    @NotNull T fromExactFloat(@NotNull ExactFloat f, @NotNull Environment env);

    /**
     * <p>toExactFloat.</p>
     *
     * @return a {@link ExactFloat} object
     */
    @NotNull ExactFloat toExactFloat();

    /**
     * <p>negate.</p>
     *
     * @return a T object
     */
    @NotNull T negate();
}
