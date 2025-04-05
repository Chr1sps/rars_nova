package rars.util

import java.awt.font.TextAttribute
import java.util.*

enum class FontWeight(val weight: Float) {
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

    override fun toString(): String {
        val name = name.replace('_', ' ').lowercase(Locale.getDefault())
        return name.substring(0, 1).uppercase(Locale.getDefault()) + name.substring(1)
    }
}
