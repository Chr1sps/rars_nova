package rars.venus.run

import arrow.core.raise.either
import rars.Globals
import rars.RISCVProgram
import rars.events.AssemblyError
import rars.settings.BoolSetting
import rars.venus.FileStatus
import rars.venus.GlobalFileStatus
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import rars.venus.isEdited
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.Icon
import javax.swing.KeyStroke

/**
 * Action class for the Run -> Assemble menu item (and toolbar icon)
 */
class RunAssembleAction(
    name: String, icon: Icon?, descrip: String,
    mnemonic: Int?, accel: KeyStroke?, gui: VenusUI
) : GuiAction(name, descrip, icon, mnemonic, accel, gui) {
    /**
     * {@inheritDoc}
     */
    override fun actionPerformed(e: ActionEvent?) {
        val name: String? = this.getValue(NAME).toString()
        val messagesPane = this.mainUI.messagesPane
        val executePane = this.mainUI.mainPane.executePane
        val registersPane = this.mainUI.registersPane
        extendedAssemblerEnabled =
            Globals.BOOL_SETTINGS.getSetting(BoolSetting.EXTENDED_ASSEMBLER_ENABLED)
        warningsAreErrors =
            Globals.BOOL_SETTINGS.getSetting(BoolSetting.WARNINGS_ARE_ERRORS)
        val globalStatus = GlobalFileStatus.get()
        if (globalStatus is FileStatus.Existing) {
            if (globalStatus.isEdited()) {
                this.mainUI.editor.save()
            }
            val systemFile = globalStatus.file
            either<AssemblyError, Unit> {
                Globals.PROGRAM = RISCVProgram()
                val filesToAssembleNew = mutableListOf<File>()
                if (Globals.BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) {
                    // setting calls 
                    // for multiple
                    // file assembly
                    filesToAssembleNew.addAll(
                        systemFile.parentFile.listFiles { file ->
                            file.extension in Globals.FILE_EXTENSIONS
                        }
                    )
                } else {
                    filesToAssembleNew.add(systemFile)
                }
                if (Globals.BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_OPEN)) {
                    this@RunAssembleAction.mainUI.editor.saveAll()
                    val paths: List<File> =
                        this@RunAssembleAction.mainUI.editor.openFilePaths
                    for (path in paths) {
                        if (!filesToAssembleNew.contains(path)) {
                            filesToAssembleNew.add(path)
                        }
                    }
                }
                val useExceptionHandler =
                    Globals.BOOL_SETTINGS.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED)
                val isExceptionHandlerSet =
                    !Globals.OTHER_SETTINGS.exceptionHandler.isEmpty()
                val exceptionHandler =
                    if (useExceptionHandler && isExceptionHandlerSet)
                        File(Globals.OTHER_SETTINGS.exceptionHandler)
                    else
                        null
                programsToAssemble = Globals.PROGRAM!!.prepareFilesForAssembly(
                    filesToAssembleNew,
                    systemFile, exceptionHandler
                ).bind()
                messagesPane.postMessage(
                    buildFileNameList(
                        "$name: assembling ",
                        programsToAssemble
                    )
                )
                val warnings = Globals.PROGRAM!!.assemble(
                    programsToAssemble,
                    extendedAssemblerEnabled,
                    warningsAreErrors
                ).bind()
                if (warnings.warningsOccurred()) {
                    messagesPane.postMessage(warnings.generateWarningReport())
                }
                messagesPane.postMessage(
                    "$name: operation completed successfully.\n\n"
                )
                GlobalFileStatus.set(FileStatus.Existing.Runnable(systemFile))

                Globals.REGISTER_FILE.resetRegisters()
                Globals.FP_REGISTER_FILE.resetRegisters()
                Globals.CS_REGISTER_FILE.resetRegisters()
                Globals.INTERRUPT_CONTROLLER.reset()

                executePane.apply {
                    textSegment.setupTable()
                    dataSegment.apply {
                        setupTable()
                        highlightCellForAddress(Globals.MEMORY_INSTANCE.memoryConfiguration.dataBaseAddress)
                        clearHighlighting()
                    }
                    textSegment.apply {
                        codeHighlighting = true
                        highlightStepAtPC()
                    }
                }
                registersPane.apply {
                    registersWindow.clearWindow()
                    floatingPointWindow.clearWindow()
                    controlAndStatusWindow.clearWindow()
                }
                mainUI.apply {
                    isMemoryReset = true
                    isExecutionStarted = false
                    mainPane.selectedComponent = executePane
                    // Ensure that I/O "file descriptors" are initialized for a new program run
                    venusIO.resetFiles()
                }

            }.onLeft { assemblyError ->
                val errorReport =
                    assemblyError.errors.generateErrorAndWarningReport()
                messagesPane.postMessage(errorReport)
                messagesPane.postMessage(
                    "$name: operation completed with errors.\n\n"
                )
                // Select editor line containing first error, and corresponding error message.
                val errorMessages = assemblyError.errors.errorMessages
                for (em in errorMessages) {
                    // No line or position may mean File Not Found (e.g. exception file). Don't try
                    // to open.
                    if (em.lineNumber == 0 && em.position == 0) {
                        continue
                    }
                    if (!em.isWarning || warningsAreErrors) {
                        this.mainUI.messagesPane.selectErrorMessage(
                            em.file!!, em.lineNumber,
                            em.position
                        )
                        // Bug workaround: Line selection does not work correctly for the JEditTextArea
                        // editor
                        // when the file is opened then automatically assembled (assemble-on-open
                        // setting).
                        // Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
                        // this method (actionPerformed) explicitly with null argument. Thus e!=null
                        // test.
                        if (e != null) {
                            this.mainUI.mainPane.editTabbedPane.selectEditorTextLine(
                                em.file,
                                em.lineNumber
                            )
                        }
                        break
                    }
                }
                GlobalFileStatus.set(FileStatus.Existing.NotEdited(systemFile))
            }
        }
    }

    companion object {
        // Threshold for adding file to printed message of files being assembled.
        private const val LINE_LENGTH_LIMIT = 60
        private lateinit var programsToAssemble: List<RISCVProgram>
        var extendedAssemblerEnabled: Boolean = false
            private set
        var warningsAreErrors: Boolean = false
            private set

        // These are both used by RunResetAction to re-assemble under identical
        // conditions.
        /**
         *
         * Getter for the field `programsToAssemble`.
         *
         * @return a [ArrayList] object
         */
        @JvmStatic
        fun getProgramsToAssemble(): List<RISCVProgram> = programsToAssemble

        // Handy little utility for building comma-separated list of filenames
        // while not letting line length get out of hand.
        private fun buildFileNameList(
            preamble: String,
            programList: List<RISCVProgram>
        ): String {
            val result = StringBuilder(preamble)
            var lineLength = result.length
            for (i in programList.indices) {
                val file = programList[i].file
                val fileName = file!!.getName()
                result.append(fileName)
                    .append(if (i < programList.size - 1) ", " else "")
                lineLength += fileName.length
                if (lineLength > LINE_LENGTH_LIMIT) {
                    result.append("\n")
                    lineLength = 0
                }
            }
            return result.toString() + (if (lineLength == 0) "" else "\n") + "\n"
        }
    }
}
