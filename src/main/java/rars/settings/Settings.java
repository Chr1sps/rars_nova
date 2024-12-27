package rars.settings;

import org.jetbrains.annotations.NotNull;

import java.util.prefs.Preferences;

import static java.util.prefs.Preferences.userNodeForPackage;

public final class Settings {
    public static final @NotNull Preferences SETTINGS_PREFERENCES = userNodeForPackage(Settings.class);
    public static @NotNull BoolSettings BOOL_SETTINGS;
    public static @NotNull RuntimeTableHighlightingSettings RUNTIME_TABLE_HIGHLIGHTING_SETTINGS;
    public static @NotNull OtherSettings OTHER_SETTINGS;

    static {
        FontSettings.FONT_SETTINGS = new FontSettings(SETTINGS_PREFERENCES);
        BOOL_SETTINGS = new BoolSettings(SETTINGS_PREFERENCES);
        RUNTIME_TABLE_HIGHLIGHTING_SETTINGS = new RuntimeTableHighlightingSettings(SETTINGS_PREFERENCES);
        OTHER_SETTINGS = new OtherSettings(SETTINGS_PREFERENCES);
    }

    private Settings() {

    }
}
