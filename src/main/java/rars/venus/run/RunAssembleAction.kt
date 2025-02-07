package rars.venus.run

import arrow.core.raise.either
import rars.ErrorList
import rars.Globals
import rars.RISCVProgram
import rars.exceptions.AssemblyError
import rars.settings.BoolSetting
import rars.util.FilenameFinder
import rars.venus.FileStatus
import rars.venus.GuiAction
import rars.venus.VenusUI
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.Icon
import javax.swing.KeyStroke

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Run -> Assemble menu item (and toolbar icon)
 */
class RunAssembleAction(
    name: String?, icon: Icon?, descrip: String?,
    mnemonic: Int?, accel: KeyStroke?, gui: VenusUI
) : GuiAction(name, icon, descrip, mnemonic, accel, gui) {
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
        if (FileStatus.systemFile != null) {
            if (FileStatus.getSystemState() == FileStatus.State.EDITED) {
                this.mainUI.editor.save()
            }
            either<AssemblyError, Unit> {
                Globals.PROGRAM = RISCVProgram()
                val filesToAssembleNew = mutableListOf<File>()
                if (Globals.BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) { // setting calls 
                    // for multiple
                    // file assembly
                    filesToAssembleNew.addAll(
                        FilenameFinder.getFilenameListForDirectory(
                            FileStatus.systemFile!!.getParentFile(), Globals.fileExtensions
                        )
                    )
                } else {
                    filesToAssembleNew.add(FileStatus.systemFile!!)
                }
                if (Globals.BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_OPEN)) {
                    this@RunAssembleAction.mainUI.editor.saveAll()
                    val paths: List<File> = this@RunAssembleAction.mainUI.editor.openFilePaths
                    for (path in paths) {
                        if (!filesToAssembleNew.contains(path)) {
                            filesToAssembleNew.add(path)
                        }
                    }
                }
                val useExceptionHandler = Globals.BOOL_SETTINGS.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED)
                val isExceptionHandlerSet = !Globals.OTHER_SETTINGS.exceptionHandler.isEmpty()
                val exceptionHandler = if (useExceptionHandler && isExceptionHandlerSet)
                    File(Globals.OTHER_SETTINGS.exceptionHandler)
                else
                    null
                programsToAssemble = Globals.PROGRAM!!.prepareFilesForAssembly(
                    filesToAssembleNew,
                    FileStatus.systemFile!!, exceptionHandler
                ).bind()
                messagesPane.postMessage(
                    buildFileNameList(
                        "$name: assembling ",
                        programsToAssemble
                    )
                )
                // added logic to receive any warnings and output them.... DPS 11/28/06
                val warnings: ErrorList = Globals.PROGRAM!!.assemble(
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
                FileStatus.setAssembled(true)
                FileStatus.setSystemState(FileStatus.State.RUNNABLE)

                Globals.REGISTER_FILE.resetRegisters()
                Globals.FP_REGISTER_FILE.resetRegisters()
                Globals.CS_REGISTER_FILE.resetRegisters()
                Globals.INTERRUPT_CONTROLLER.reset()

                executePane.textSegment.setupTable()
                executePane.dataSegment.setupTable()
                executePane.dataSegment.highlightCellForAddress(Globals.MEMORY_INSTANCE.memoryConfiguration.dataBaseAddress)
                executePane.dataSegment.clearHighlighting()
                executePane.labelValues.setupTable()
                executePane.textSegment.codeHighlighting = true
                executePane.textSegment.highlightStepAtPC()
                registersPane.registersWindow.clearWindow()
                registersPane.floatingPointWindow.clearWindow()
                registersPane.controlAndStatusWindow.clearWindow()
                this@RunAssembleAction.mainUI.isMemoryReset = true
                this@RunAssembleAction.mainUI.isExecutionStarted = false
                this@RunAssembleAction.mainUI.mainPane.setSelectedComponent(executePane)

                // Aug. 24, 2005 Ken Vollmar

                // Ensure that I/O "file descriptors" are initialized for a new program run
                this@RunAssembleAction.mainUI.venusIO.resetFiles()
            }.onLeft { assemblyError ->
                val errorReport = assemblyError.errors.generateErrorAndWarningReport()
                messagesPane.postMessage(errorReport)
                messagesPane.postMessage(
                    "$name: operation completed with errors.\n\n"
                )
                // Select editor line containing first error, and corresponding error message.
                val errorMessages = assemblyError.errors.errorMessages
                for (em in errorMessages) {
                    // No line or position may mean File Not Found (e.g. exception file). Don't try
                    // to open. DPS 3-Oct-2010
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
                        // DPS 9-Aug-2010
                        if (e != null) {
                            this.mainUI.mainPane.editTabbedPane.selectEditorTextLine(em.file, em.lineNumber)
                        }
                        break
                    }
                }
                FileStatus.setAssembled(false)
                FileStatus.setSystemState(FileStatus.State.NOT_EDITED)
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
                result.append(fileName).append(if (i < programList.size - 1) ", " else "")
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
