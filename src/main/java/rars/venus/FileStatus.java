package rars.venus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;

import java.io.File;

/**
 * Used to store and return information on the status of the current ASM file
 * that
 * is being edited in the program.
 *
 * @author Team JSpim
 */
public final class FileStatus {

    public static @Nullable File systemFile = null;
    private static @NotNull FileStatus.State systemState = State.NO_FILE;
    private static boolean systemAssembled = false;
    private @NotNull FileStatus.State status;
    private @Nullable File file;

    /**
     * Create a FileStatus object with FileStatus.NO_FILE for status and null for
     * file getters.
     */
    public FileStatus() {
        this(State.NO_FILE, null);
    }

    /**
     * Create a FileStatus object with given status and file pathname.
     *
     * @param status
     *     Initial file status. See FileStatus static constants.
     * @param file
     *     File object representing the file.
     */
    public FileStatus(final @NotNull FileStatus.State status, final @Nullable File file) {
        this.status = status;
        this.file = file;
    }

    /**
     * Get file status
     *
     * @return file status EDITED, RUNNABLE, etc, see list above
     */
    public static @NotNull FileStatus.State getSystemState() {
        return systemState;
    }

    /**
     * Set file status. Also updates menu state accordingly.
     *
     * @param newStatus
     *     New status: EDITED, RUNNABLE, etc, see list above.
     */
    public static void setSystemState(final @NotNull FileStatus.State newStatus) {
        systemState = newStatus;
        if (Globals.GUI != null) {
            Globals.GUI.setMenuState(systemState);
        }
    }

    /**
     * Tells whether the file has been assembled.
     *
     * @return Boolean value that is true if the ASM file has been assembled.
     */
    public static boolean isAssembled() {
        return systemAssembled;
    }

    /**
     * Changes the value of assenbked to the parameter given.
     *
     * @param b
     *     boolean variable that tells what to set assembled to.
     */
    public static void setAssembled(final boolean b) {
        systemAssembled = b;
    }

    /**
     * Resets all the values in FileStatus
     */
    public static void reset() {
        systemState = State.NO_FILE;
        systemAssembled = false;
        systemFile = null;
    }

    /**
     * Get editing status of this file.
     *
     * @return current editing status. See FileStatus static constants.
     */
    public @NotNull FileStatus.State getFileStatus() {
        return this.status;
    }

    /**
     * Set editing status of this file. See FileStatus static constants.
     *
     * @param newStatus
     *     the new status
     */
    public void setFileStatus(final @NotNull FileStatus.State newStatus) {
        this.status = newStatus;
    }

    /**
     * Determine if file is "new", which means created using New but not yet saved.
     * If created using Open, it is not new.
     *
     * @return true if file was created using New and has not yet been saved, false
     * otherwise.
     */
    public boolean isNew() {
        return switch (status) {
            case NEW_NOT_EDITED, NEW_EDITED -> true;
            default -> false;
        };
    }

    /**
     * Determine if file has been modified since last save or, if not yet saved,
     * since
     * being created using New or Open.
     *
     * @return true if file has been modified since save or creation, false
     * otherwise.
     */
    public boolean hasUnsavedEdits() {
        return switch (status) {
            case NEW_EDITED, EDITED -> true;
            default -> false;
        };
    }

    /**
     * Get file name with no path information. See java.io.File.getName()
     *
     * @return file as a String
     */
    public @Nullable File getFile() {
        return this.file;
    }

    public void setFile(final @Nullable File file) {
        this.file = file;
    }

    /**
     * Update static FileStatus fields with values from this FileStatus object
     * To support legacy code that depends on the static.
     */
    public void updateStaticFileStatus() {
        systemState = this.status;
        systemAssembled = false;
        systemFile = this.file;
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
        OPENING;

        public boolean isAssembled() {
            return switch (this) {
                case RUNNABLE, RUNNING, TERMINATED -> true;
                default -> false;
            };
        }
    }

}
