package rars.settings;

import org.jetbrains.annotations.NotNull;

import static java.util.prefs.Preferences.*;

public final class Settings {
    private Settings() {
        
    }
    
    public static @NotNull EditorThemeSettings editorThemeSettings;
    public static @NotNull FontSettings fontSettings;
    public static @NotNull BoolSettings boolSettings;
    public static @NotNull RuntimeTableHighlightingSettings runtimeTableHighlightingSettings;
    public static @NotNull OtherSettings otherSettings;
    
    static {
        final var preferences = userNodeForPackage(Settings.class);
        editorThemeSettings = new EditorThemeSettings(preferences);
        fontSettings = new FontSettings(preferences);
        boolSettings = new BoolSettings(preferences);
        runtimeTableHighlightingSettings = new RuntimeTableHighlightingSettings(preferences);
        otherSettings = new OtherSettings(preferences);
    }
}
