package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TokenStyle;

import java.awt.*;

public final class HighlightingDefaults {
    public static final @NotNull TokenStyle DEFAULT_TEXT_SEGMENT_STYLE = fromBackground(new Color(0xFFFF99));
    public static final @NotNull TokenStyle DEFAULT_DATA_SEGMENT_STYLE = fromBackground(new Color(0x99CCFF));
    public static final @NotNull TokenStyle DEFAULT_REGISTER_STYLE = fromBackground(new Color(0x99CC55));

    private HighlightingDefaults() {
    }

    private static @NotNull TokenStyle fromBackground(final @NotNull Color color) {
        return new TokenStyle(Color.BLACK, color, false, false, false);
    }
}
