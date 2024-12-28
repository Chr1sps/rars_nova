package rars.settings;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.prefs.Preferences;

import static java.util.prefs.Preferences.userNodeForPackage;

/**
 * A base class for Settings objects.
 */
public abstract class SettingsBase {
    /**
     * The Preferences node for all the RARS settings.
     */
    protected static final @NotNull Preferences SETTINGS_PREFERENCES = userNodeForPackage(SettingsBase.class);
    private final @NotNull HashSet<@NotNull Runnable> listeners;

    protected SettingsBase() {
        listeners = new HashSet<>();
    }

    public void addChangeListener(final @NotNull Runnable listener) {
        this.listeners.add(listener);
    }

    public void addChangeListener(final @NotNull Runnable listener, final boolean runImmediately) {
        if (runImmediately) {
            // noinspection unchecked
            listener.run();
        }
        this.listeners.add(listener);
    }

    public void removeChangeListener(final @NotNull Runnable listener) {
        this.listeners.remove(listener);
    }

    protected void submit() {
        for (final var listener : listeners) {
            // noinspection unchecked
            listener.run();
        }
    }
}
