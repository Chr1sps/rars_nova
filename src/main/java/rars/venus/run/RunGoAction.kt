package rars.venus.run

import rars.Globals
import rars.notices.SimulatorNotice
import rars.settings.BoolSetting
import rars.simulator.StoppingEvent
import rars.simulator.isDone
import rars.simulator.storeProgramArguments
import rars.util.Listener
import rars.venus.ExecutePane
import rars.venus.FileStatus
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import java.awt.EventQueue
import java.awt.event.ActionEvent
import javax.swing.Icon
import javax.swing.JOptionPane
import javax.swing.KeyStroke

/**
 * Action class for the Run -> Go menu item (and toolbar icon)
 */
class RunGoAction(
    name: String, icon: Icon?, descrip: String,
    mnemonic: Int?, accel: KeyStroke?, gui: VenusUI
) : GuiAction(name, descrip, icon, mnemonic, accel, gui) {
    private var name: String? = null
    private var executePane: ExecutePane? = null

    /**
     * {@inheritDoc}
     *
     *
     * Action to take when GO is selected -- run the MIPS program!
     */
    override fun actionPerformed(e: ActionEvent?) {
        this.name = this.getValue(NAME).toString()
        this.executePane = this.mainUI.mainPane.executePane
        if (FileStatus.isAssembled()) {
            if (!this.mainUI.isExecutionStarted) {
                this.processProgramArgumentsIfAny()
            }
            if (this.mainUI.isMemoryReset || this.mainUI.isExecutionStarted) {

                this.mainUI.isExecutionStarted = true

                this.mainUI.messagesPane.postMessage(
                    this.name + ": running " + FileStatus.systemFile!!.getName() + "\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
                this.executePane!!.textSegment.codeHighlighting = false
                this.executePane!!.textSegment.unhighlightAllSteps()
                // FileStatus.set(FileStatus.RUNNING);
                this.mainUI.setMenuState(FileStatus.State.RUNNING)

                // Setup cleanup procedures for the simulation
                val onSimulatorStopListener =
                    object : Listener<SimulatorNotice> {
                        override fun invoke(notice: SimulatorNotice) {
                            if (notice.action != SimulatorNotice.Action.STOP) {
                                return
                            }
                            val event = notice.event
                            when (event) {
                                StoppingEvent.UserPaused,
                                StoppingEvent.BreakpointHit -> EventQueue.invokeLater {
                                    paused(event)
                                }
                                else -> EventQueue.invokeLater {
                                    stopped(event)
                                }
                            }
                            Globals.SIMULATOR
                                .simulatorNoticeHook
                                .unsubscribe(this)
                        }

                    }
                Globals.SIMULATOR.simulatorNoticeHook.subscribe(
                    onSimulatorStopListener
                )

                val breakPoints =
                    this.executePane!!.textSegment.getSortedBreakPointsArray()
                Globals.SIMULATOR.startSimulation(
                    Globals.REGISTER_FILE.programCounter,
                    maxSteps,
                    breakPoints,
                    this.mainUI
                )
            } else {
                // This should never occur because at termination the Go and Step buttons are
                // disabled.
                JOptionPane.showMessageDialog(
                    this.mainUI,
                    "reset " + this.mainUI.isMemoryReset + " started " + this.mainUI.isExecutionStarted
                ) // "You
                // must
                // reset
                // before
                // you
                // can
                // execute
                // the
                // program
                // again.");
            }
        } else {
            // note: this should never occur since "Go" is only enabled after successful
            // assembly.
            JOptionPane.showMessageDialog(
                this.mainUI,
                "The program must be assembled before it can be run."
            )
        }
    }

    /**
     * Method to be called when Pause is selected through menu/toolbar/shortcut.
     * This should only
     * happen when MIPS program is running (FileStatus.RUNNING). See VenusUI.java
     * for enabled
     * status of menu items based on FileStatus. Set GUI as if at breakpoint or
     * executing
     * step by step.
     */
    fun paused(event: StoppingEvent) {
        // I doubt this can happen (pause when execution finished), but if so treat it
        // as stopped.
        if (event.isDone) {
            stopped(event)
            return
        }
        if (event == StoppingEvent.BreakpointHit) {
            this.mainUI.messagesPane.postMessage(
                name + ": execution paused at breakpoint: " + FileStatus.systemFile!!.getName() + "\n\n"
            )
        } else {
            this.mainUI.messagesPane.postMessage(
                name + ": execution paused by user: " + FileStatus.systemFile!!.getName() + "\n\n"
            )
        }
        this.mainUI.messagesPane.selectMessageTab()
        this.executePane!!.textSegment.codeHighlighting = true
        this.executePane!!.textSegment.highlightStepAtPC()
        this.executePane!!.registerValues.updateRegisters()
        this.executePane!!.fpRegValues.updateRegisters()
        this.executePane!!.csrValues.updateRegisters()
        this.executePane!!.dataSegment.updateValues()
        FileStatus.setSystemState(FileStatus.State.RUNNABLE)
        this.mainUI.isMemoryReset = false
    }

    /**
     * Method to be called when Stop is selected through menu/toolbar/shortcut. This
     * should only
     * happen when MIPS program is running (FileStatus.RUNNING). See VenusUI.java
     * for enabled
     * status of menu items based on FileStatus. Display finalized values as if
     * execution
     * terminated due to completion or exception.
     */
    fun stopped(stoppingEvent: StoppingEvent) {
        // show final register and data segment values.
        this.executePane!!.registerValues.updateRegisters()
        this.executePane!!.fpRegValues.updateRegisters()
        this.executePane!!.csrValues.updateRegisters()
        this.executePane!!.dataSegment.updateValues()
        FileStatus.setSystemState(FileStatus.State.TERMINATED)
        this.mainUI.venusIO.resetFiles() // close any files opened in MIPS program
        // Bring CSRs to the front if terminated due to exception.
        if (stoppingEvent is StoppingEvent.ErrorHit) {
            this.mainUI.registersPane.setSelectedComponent(this.executePane!!.csrValues)
            this.executePane!!.textSegment.codeHighlighting = true
            this.executePane!!.textSegment.unhighlightAllSteps()
            this.executePane!!.textSegment.highlightStepAtAddress(Globals.REGISTER_FILE.programCounter - 4)
        }
        when (stoppingEvent) {
            StoppingEvent.NormalTermination -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution completed successfully.\n\n"
                )
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (" + Globals.exitCode + ") --\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
            }

            StoppingEvent.CliffTermination -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by null instruction.\n\n"
                )
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (dropped off bottom) --\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
            }

            is StoppingEvent.ErrorHit -> {
                this.mainUI.messagesPane.postMessage(
                    stoppingEvent.error.message.generateReport()
                )
                this.mainUI.messagesPane.postMessage(
                    "\n${this.name}: execution terminated with errors.\n\n"
                )
            }

            StoppingEvent.UserStopped -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by user.\n\n"
                )
                this.mainUI.messagesPane.selectMessageTab()
            }

            StoppingEvent.MaxStepsHit -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution step limit of " + maxSteps + " exceeded.\n\n"
                )
                this.mainUI.messagesPane.selectMessageTab()
            }

            else -> error("Should not be invoking this method for this event: $stoppingEvent")
        }
        resetMaxSteps()
        this.mainUI.isMemoryReset = false
    }

    /**
     * Method to store any program arguments into MIPS memory and registers before
     * execution begins. Arguments go into the gap between $sp and kernel memory.
     * Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
     * $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
     */
    private fun processProgramArgumentsIfAny() {
        val programArguments = this.executePane!!.textSegment.programArguments
        if (programArguments == null || programArguments.isEmpty() || !Globals.BOOL_SETTINGS.getSetting(
                BoolSetting.PROGRAM_ARGUMENTS
            )
        ) {
            return
        }
        storeProgramArguments(programArguments)
    }

    companion object {
        const val DEFAULT_MAX_STEPS: Int = -1
        var maxSteps: Int = DEFAULT_MAX_STEPS

        /**
         * Reset max steps limit to default value at termination of a simulated
         * execution.
         */
        @JvmStatic
        fun resetMaxSteps() {
            maxSteps = DEFAULT_MAX_STEPS
        }
    }
}
