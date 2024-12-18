package rars.settings;

import org.jetbrains.annotations.NotNull;

import static java.util.prefs.Preferences.userNodeForPackage;

public final class Settings {
    public static @NotNull EditorThemeSettings EDITOR_THEME_SETTINGS;
    public static @NotNull FontSettings FONT_SETTINGS;
    public static @NotNull BoolSettings BOOL_SETTINGS;
    public static @NotNull RuntimeTableHighlightingSettings RUNTIME_TABLE_HIGHLIGHTING_SETTINGS;
    public static @NotNull OtherSettings OTHER_SETTINGS;

    static {
        final var preferences = userNodeForPackage(Settings.class);
        EDITOR_THEME_SETTINGS = new EditorThemeSettings(preferences);
        FONT_SETTINGS = new FontSettings(preferences);
        BOOL_SETTINGS = new BoolSettings(preferences);
        RUNTIME_TABLE_HIGHLIGHTING_SETTINGS = new RuntimeTableHighlightingSettings(preferences);
        OTHER_SETTINGS = new OtherSettings(preferences);
    }

    private Settings() {

    }
}
