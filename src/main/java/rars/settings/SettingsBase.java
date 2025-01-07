package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.util.ListenerDispatcher;

/**
 * A base class for Settings objects.
 */
public abstract class SettingsBase {
    /** A hook for listeners to be notified when the settings change. */
    public final @NotNull ListenerDispatcher<Void>.Hook onChangeListenerHook;
    /** Internal dispatcher for submiting setting change events. */
    protected final @NotNull ListenerDispatcher<Void> onChangeDispatcher;

    protected SettingsBase() {
        this.onChangeDispatcher = new ListenerDispatcher<>();
        this.onChangeListenerHook = this.onChangeDispatcher.getHook();
    }
}
