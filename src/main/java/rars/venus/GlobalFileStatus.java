package rars.venus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;

/**
 * Used to store and return information on the status of the current ASM file
 * that
 * is being edited in the program.
 *
 * @author Team JSpim
 */
@Deprecated(forRemoval = true)
public final class GlobalFileStatus {

    private static @Nullable FileStatus systemStatus = null;

    public static @Nullable FileStatus get() {
        return systemStatus;
    }

    public static void set(final @NotNull FileStatus status) {
        systemStatus = status;
        if (Globals.GUI != null) {
            Globals.GUI.setMenuState(status);
        }
    }

    public static void reset() {
        systemStatus = null;
    }

    public enum State {
        /// Initial state or after close.
        NO_FILE,
        /// New edit window with no edits.
        NEW_NOT_EDITED,
        /// New edit window with unsaved edits.
        NEW_EDITED,
        /// Open/saved edit window with no edits.
        NOT_EDITED,
        /// Open/saved edit window with unsaved edits.
        EDITED,
        /// Successful assembly.
        RUNNABLE,
        /// Execution is under way
        RUNNING,
        /// Execution terminated.
        TERMINATED,
        /// File is being opened.
        OPENING
    }
}
