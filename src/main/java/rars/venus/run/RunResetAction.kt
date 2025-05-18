package rars.venus.run

import rars.Globals
import rars.venus.FileStatus
import rars.venus.GlobalFileStatus
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import java.awt.event.ActionEvent
import javax.swing.Icon
import javax.swing.KeyStroke

/**
 * Action for the Run -> Reset menu item
 */
class RunResetAction(
    name: String, icon: Icon?, descrip: String,
    mnemonic: Int?, accel: KeyStroke?, gui: VenusUI
) : GuiAction(name, descrip, icon, mnemonic, accel, gui) {
    /**
     * {@inheritDoc}
     *
     *
     * reset GUI components and MIPS resources
     */
    override fun actionPerformed(e: ActionEvent?) {
        RunGoAction.resetMaxSteps()
        val name = this.getValue(NAME).toString()
        val executePane = mainUI.mainPane.executePane

        // The difficult part here is resetting the data segment. Two approaches are:
        // 1. After each assembly, get a deep copy of the Globals.memory array
        // containing data segment. Then replace it upon reset.
        // 2. Simply re-assemble the program upon reset, and the assembler will
        // build a new data segment. Reset can only be done after a successful
        // assembly, so there is "no" chance of assembler error.
        // I am choosing the second approach although it will slow down the reset
        // operation. The first approach requires additional Memory class methods.
        Globals.PROGRAM!!.assemble(
            RunAssembleAction.getProgramsToAssemble(),
            RunAssembleAction.extendedAssemblerEnabled,
            RunAssembleAction.warningsAreErrors
        ).onLeft {
            // Should not be possible
            mainUI.messagesPane.postMessage( // pe.errors().generateErrorReport());
                "Unable to reset. Please close file then re-open and re-assemble.\n"
            )
            return
        }

        Globals.REGISTER_FILE.resetRegisters()
        Globals.FP_REGISTER_FILE.resetRegisters()
        Globals.CS_REGISTER_FILE.resetRegisters()
        Globals.INTERRUPT_CONTROLLER.reset()

        executePane.apply {
            registerValues.apply {
                clearHighlighting()
                updateRegisters()
            }
            fpRegValues.apply {
                clearHighlighting()
                updateRegisters()
            }
            csrValues.apply {
                clearHighlighting()
                updateRegisters()
            }
            dataSegment.apply {
                highlightCellForAddress(Globals.MEMORY_INSTANCE.memoryConfiguration.dataBaseAddress)
                clearHighlighting()
            }
            textSegment.apply {
                resetModifiedSourceCode()
                codeHighlighting = true
                highlightStepAtPC()
            }
        }
        GlobalFileStatus.set(
            FileStatus.Existing.Runnable(
                (GlobalFileStatus.get()!! as FileStatus.Existing).file
            )
        )
        mainUI.run {
            registersPane.setSelectedComponent(executePane.registerValues)
            isMemoryReset = true
            isExecutionStarted = false
            venusIO.resetFiles() // Ensure that I/O "file descriptors" are initialized for a new program run
            messagesPane.postRunMessage(
                """
                |$name: reset completed.
                |
                """.trimMargin()
            )
        }
    }
}
