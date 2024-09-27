package rars.notices;

public final class SettingsNotice implements Notice {
    private static final SettingsNotice INSTANCE = new SettingsNotice();

    private SettingsNotice() {
    }

    public static SettingsNotice get() {
        return SettingsNotice.INSTANCE;
    }

}
