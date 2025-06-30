package rars.venus

import java.io.File

/**
 * Manage the file being edited.
 * Currently only manages one file at a time, but can be expanded.
 */
class Editor(private val mainUI: VenusUI) {
    private val mainUIbaseTitle: String = mainUI.title

    // Current Directory for Open operation, same for Save operation
    // Values will mainly be set by the EditTabbedPane as Open/Save operations
    // occur.
    private val defaultOpenDirectory: String = System.getProperty("user.dir")
    private val defaultSaveDirectory: String = System.getProperty("user.dir")

    /*
     * number of times File->New has been selected. Used to generate
     * default file until first Save or Save As.
     */
    private var newUsageCount = 0


    lateinit var editTabbedPane: EditTabbedPane

    val openFilePaths: List<File>
        get() = editTabbedPane.openFilePaths

    /** Name of the current directory for the `Open` operation. */
    var currentOpenDirectory: String = defaultOpenDirectory
        set(newValue) {
            field = newValue.let {
                it.takeIfPathIsADirectory() ?: defaultOpenDirectory
            }
        }

    /**
     * Name of the current directory for the `Save` and `Save As` operations.
     */
    var currentSaveDirectory: String = defaultSaveDirectory
        set(newValue) {
            field = newValue.let {
                it.takeIfPathIsADirectory() ?: defaultSaveDirectory
            }
        }

    val nextDefaultFilename: String
        /**
         * Generates a default file name
         *
         * @return returns string mipsN.asm, where N is 1,2,3,...
         */
        get() {
            this.newUsageCount++
            return "riscv" + this.newUsageCount + ".asm"
        }

    /*
     * Places name of file currently being edited into its edit tab and
     * the application's title bar. The edit tab will contain only
     * the file, the title bar will contain full pathname.
     * If file has been modified since created, opened or saved, as
     * indicated by value of the status parameter, the name and path
     * will be followed with an '*'. If newly-created file has not
     * yet been saved, the title bar will show (temporary) file name
     * but not path.
     *
     * @param path
     *     Full pathname for file
     * @param name
     *     Name of file (last component of path)
     * @param status
     *     Edit status of file. See FileStatus static constants.
     */
    fun setTitleFromFileStatus(
        fileStatus: FileStatus
    ) {
        val editIndicator = if (fileStatus.isEdited()) "*" else ""
        val titleName: String = when (fileStatus) {
            is FileStatus.New -> fileStatus.tmpName
            is FileStatus.Existing -> fileStatus.file.absolutePath
        }
        val tabName: String = when (fileStatus) {
            is FileStatus.New -> fileStatus.tmpName
            is FileStatus.Existing -> fileStatus.file.getName()
        }
        mainUI.title = "$titleName$editIndicator - $mainUIbaseTitle"
        val tabbedPane = editTabbedPane.tabbedPane
        tabbedPane.setTitleAt(
            tabbedPane.selectedIndex,
            tabName + editIndicator
        )
    }

    /**
     * Perform "new" operation to create an empty tab.
     */
    fun newFile(): Unit = editTabbedPane.newFile()

    /**
     * Perform "close" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    fun close(): Boolean = editTabbedPane.closeCurrentFile()

    /**
     * Close all currently open files.
     *
     * @return true if succeeded, else false.
     */
    fun closeAll(): Boolean = editTabbedPane.closeAllFiles()

    /**
     * Perform "save" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    fun save(): Boolean = editTabbedPane.saveCurrentFile()

    /**
     * Perform "save as" operation on current tab's file.
     *
     * @return true if succeeded, else false.
     */
    fun saveAs(): Boolean = editTabbedPane.saveAsCurrentFile()

    /**
     * Perform save operation on all open files (tabs).
     *
     * @return true if succeeded, else false.
     */
    fun saveAll(): Boolean = editTabbedPane.saveAllFiles()

    /**
     * Open file in a new tab.
     *
     * @return true if succeeded, else false.
     */
    fun openFile(): Boolean = editTabbedPane.openFile()

    /**
     * Open files in new tabs.
     *
     * @param paths
     * File paths to open
     * @return true if succeeded, else false.
     */
    fun openPaths(paths: List<String>): Boolean =
        paths.map { File(it) }.let(::openFiles)

    /**
     * Open files in new tabs.
     *
     * @param files
     * Files to open
     * @return true if succeeded, else false.
     */
    fun openFiles(files: List<File>): Boolean = files.all {
        editTabbedPane.openFile(it)
    }

    /**
     * Called by several of the Action objects when there is potential
     * loss of editing changes. Specifically: if there is a current
     * file open for editing and its modify flag is true, then give user
     * a dialog box with choice to save, discard edits, or cancel and
     * carry out the decision. This applies to File->New, File->Open,
     * File->Close, and File->Exit.
     *
     * @return false means user selected Cancel so caller should do that.
     * Return of true means caller can proceed (edits were saved or
     * discarded).
     */
    fun editsSavedOrAbandoned(): Boolean =
        editTabbedPane.editsSavedOrAbandoned()

    companion object {
        const val MIN_TAB_SIZE: Int = 1
        const val MAX_TAB_SIZE: Int = 32
        const val MIN_BLINK_RATE: Int = 0 // no flashing
        const val MAX_BLINK_RATE: Int = 1000 // once per value
    }
}

private fun String.takeIfPathIsADirectory(): String? = takeIf {
    val file = File(it)
    file.exists() && file.isDirectory
}