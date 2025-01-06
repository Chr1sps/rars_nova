package rars.venus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;

import java.io.File;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Used to store and return information on the status of the current ASM file
 * that
 * is being edited in the program.
 *
 * @author Team JSpim
 */
public final class FileStatus {

    private static @NotNull FileStatus.State systemStatus = State.NO_FILE;

    // The static part. Legacy code from original student team's
    // 2003 Practicum project through MARS 3.8, when the editor
    // was limited to one file. The status of that file became
    // the de facto status of the system. Should have used a
    // singleton class but in 2003 did not know what that was!
    // My plan is to phase out all statics but the constants
    // in MARS 4.0 but will keep it in place while at the same time
    // defining non-static members for use by individual files
    // currently opened in the editor. DPS, 9 April 2010.
    private static boolean systemAssembled;
    private static boolean systemSaved;
    private static boolean systemEdited;
    private static String systemName;
    private static @Nullable File systemFile;

    private @NotNull FileStatus.State status;
    private @Nullable File file;

    /**
     * Create a FileStatus object with FileStatis.NO_FILE for status and null for
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
     * Set file status. Also updates menu state accordingly.
     *
     * @param newStatus
     *     New status: EDITED, RUNNABLE, etc, see list above.
     */
    public static void set(final @NotNull FileStatus.State newStatus) {
        systemStatus = newStatus;
        Globals.gui.setMenuState(systemStatus);
    }

    /**
     * Get file status
     *
     * @return file status EDITED, RUNNABLE, etc, see list above
     */
    public static @NotNull FileStatus.State get() {
        return systemStatus;
    }

    /**
     * Returns the ASM file.
     *
     * @return The ASM file.
     */
    public static @Nullable File getSystemFile() {
        return systemFile;
    }

    /**
     * Sets the file to the ASM file passed.
     *
     * @param f
     *     file object variable that stores the ASM file.
     */
    public static void setSystemFile(final File f) {
        systemFile = f;
    }

    /**
     * Returns the name of the file.
     *
     * @return The name of the ASM file.
     */
    public static String getName() {
        return systemName;
    }

    /**
     * Changes the value of name to the parameter given.
     *
     * @param s
     *     string variable tells what to set the name of the file to .
     */
    public static void setName(final String s) {
        systemName = s;
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
     * Tells whether the file has been saved.
     *
     * @return Boolean variable that is true if the ASM file has been saved
     */
    public static boolean isSaved() {
        return systemSaved;
    }

    /**
     * Changes the value of saved to the parameter given.
     *
     * @param b
     *     boolean variable that tells what to set saved to .
     */
    public static void setSaved(final boolean b) {
        systemSaved = b;
    }

    /**
     * Tells whether the file has been edited since it has been saved.
     *
     * @return Boolean value that returns true if the ASM file has been edited.
     */
    public static boolean isEdited() {
        return systemEdited;
    }


    // Remaining members are of instantiable class that can be used by
    // every file that is currently open in the editor.

    /**
     * Changes the value of edited to the parameter given.
     *
     * @param b
     *     boolean variable that tells what to set edited to.
     */
    public static void setEdited(final boolean b) {
        systemEdited = b;
    }

    /**
     * Resets all the values in FileStatus
     */
    public static void reset() {
        systemStatus = State.NO_FILE;
        systemName = "";
        systemAssembled = false;
        systemSaved = false;
        systemEdited = false;
        systemFile = null;
    }

    public void setFile(final @Nullable File file) {
        this.file = file;
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

    /**
     * Update static FileStatus fields with values from this FileStatus object
     * To support legacy code that depends on the static.
     */
    public void updateStaticFileStatus() {
        systemStatus = this.status;
        systemName = this.file.getPath();
        systemAssembled = false;
        systemSaved = switch (status) {
            case NOT_EDITED, RUNNABLE, RUNNING, TERMINATED -> true;
            default -> false;
        };
        systemEdited = hasUnsavedEdits();
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
        // DPS 9-Aug-2011
        /// File is being opened.
        OPENING
    }

}
