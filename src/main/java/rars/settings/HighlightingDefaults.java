package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.Theme;
import rars.venus.editors.TokenStyle;

import java.awt.*;

public final class HighlightingDefaults {
    private static @NotNull TokenStyle fromBackground(final @NotNull Color color) {
        final var defaultTheme = Theme.getDefaultLightTheme();
        return new TokenStyle(defaultTheme.foregroundColor, color, false, false, false);
    }
    
    public static final @NotNull TokenStyle DEFAULT_TEXT_SEGMENT_STYLE = fromBackground(new Color(0xFFFF99)),
            DEFAULT_DELAY_SLOT_STYLE = fromBackground(new Color(0x33FF00)),
            DEFAULT_DATA_SEGMENT_STYLE = fromBackground(new Color(0x99CCFF)),
            DEFAULT_REGISTER_STYLE = fromBackground(new Color(0x99CC55));
    
    private HighlightingDefaults() {
    }
}
