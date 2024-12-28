package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class BoolSettings extends ListenableBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private final @NotNull Preferences preferences;
    private final @NotNull HashMap<BoolSetting, Boolean> currentSettings;

    public BoolSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        this.currentSettings = new HashMap<>();
        loadSettingsFromPreferences();
    }

    /**
     * Sets the value of a setting. Does not save the setting to persistent storage.
     *
     * @param setting The setting to change
     * @param value   The new value of the setting
     */
    public void setSetting(final @NotNull BoolSetting setting, final boolean value) {
        currentSettings.put(setting, value);
    }

    /**
     * Sets the value of a setting and immediately saves it to persistent storage.
     *
     * @param setting The setting to change
     * @param value   The new value of the setting
     */
    public void setSettingAndSave(final @NotNull BoolSetting setting, final boolean value) {
        currentSettings.put(setting, value);
        preferences.putBoolean(setting.getName(), value);
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
        submit();
    }

    /**
     * Gets the value of a setting.
     *
     * @param setting The setting to get
     * @return The value of the setting
     */
    public boolean getSetting(final @NotNull BoolSetting setting) {
        return currentSettings.get(setting);
    }

    private void loadSettingsFromPreferences() {
        for (final var setting : BoolSetting.values()) {
            final var value = preferences.getBoolean(setting.getName(), setting.getDefault());
            currentSettings.put(setting, value);
        }
    }
}
