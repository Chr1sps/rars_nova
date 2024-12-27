package rars.util;

import java.awt.font.TextAttribute;

public enum FontWeight implements Comparable<FontWeight> {

    EXTRA_LIGHT(TextAttribute.WEIGHT_EXTRA_LIGHT),
    LIGHT(TextAttribute.WEIGHT_LIGHT),
    DEMI_LIGHT(TextAttribute.WEIGHT_DEMILIGHT),
    REGULAR(TextAttribute.WEIGHT_REGULAR),
    SEMI_BOLD(TextAttribute.WEIGHT_SEMIBOLD),
    MEDIUM(TextAttribute.WEIGHT_MEDIUM),
    DEMI_BOLD(TextAttribute.WEIGHT_DEMIBOLD),
    BOLD(TextAttribute.WEIGHT_BOLD),
    HEAVY(TextAttribute.WEIGHT_HEAVY),
    EXTRA_BOLD(TextAttribute.WEIGHT_EXTRABOLD),
    ULTRA_BOLD(TextAttribute.WEIGHT_ULTRABOLD);
    public final float weight;

    FontWeight(final float weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        final var name = name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
