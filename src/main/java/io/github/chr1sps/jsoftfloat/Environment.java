package io.github.chr1sps.jsoftfloat;

import java.util.EnumSet;

/**
 * <p>Environment class.</p>
 *
 */
public class Environment {
    public EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
    public RoundingMode mode;

    /**
     * <p>Constructor for Environment.</p>
     *
     * @param mode a {@link io.github.chr1sps.jsoftfloat.RoundingMode} object
     */
    public Environment(RoundingMode mode) {
        this.mode = mode;
    }

    /**
     * <p>Constructor for Environment.</p>
     */
    public Environment() {
        this(RoundingMode.even);
    }
}
