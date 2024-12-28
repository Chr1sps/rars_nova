package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TokenStyle;

import java.awt.*;

public final class HighlightingDefaults {
    public static final @NotNull TokenStyle DEFAULT_TEXT_SEGMENT_STYLE = fromBackground(new Color(0xFFFF99)),
        DEFAULT_DELAY_SLOT_STYLE = fromBackground(new Color(0x33FF00)),
        DEFAULT_DATA_SEGMENT_STYLE = fromBackground(new Color(0x99CCFF)),
        DEFAULT_REGISTER_STYLE = fromBackground(new Color(0x99CC55));

    private HighlightingDefaults() {
    }

    private static @NotNull TokenStyle fromBackground(final @NotNull Color color) {
        final var defaultForeground = SettingsTheme.DEFAULT_THEME.foregroundColor;
        return new TokenStyle(defaultForeground, color, false, false, false);
    }
}
