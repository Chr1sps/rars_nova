package rars.venus.run

import rars.Globals
import rars.exceptions.SimulationError
import rars.notices.SimulatorNotice
import rars.settings.BoolSetting
import rars.simulator.Simulator
import rars.simulator.storeProgramArguments
import rars.util.Listener
import rars.venus.ExecutePane
import rars.venus.FileStatus
import rars.venus.GuiAction
import rars.venus.VenusUI
import java.awt.EventQueue
import java.awt.event.ActionEvent
import javax.swing.Icon
import javax.swing.JOptionPane
import javax.swing.KeyStroke

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Run -> Go menu item (and toolbar icon)
 */
class RunGoAction(
    name: String?, icon: Icon?, descrip: String?,
    mnemonic: Int?, accel: KeyStroke?, gui: VenusUI
) : GuiAction(name, icon, descrip, mnemonic, accel, gui) {
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
                this.processProgramArgumentsIfAny() // DPS 17-July-2008
            }
            if (this.mainUI.isMemoryReset || this.mainUI.isExecutionStarted) {
                // added 8/27/05

                this.mainUI.isExecutionStarted = true

                this.mainUI.messagesPane.postMessage(
                    this.name + ": running " + FileStatus.systemFile!!.getName() + "\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
                this.executePane!!.textSegment.setCodeHighlighting(false)
                this.executePane!!.textSegment.unhighlightAllSteps()
                // FileStatus.set(FileStatus.RUNNING);
                this.mainUI.setMenuState(FileStatus.State.RUNNING)

                // Setup cleanup procedures for the simulation
                val onSimulatorStopListener = object : Listener<SimulatorNotice> {
                    override fun invoke(notice: SimulatorNotice) {
                        if (notice.action != SimulatorNotice.Action.STOP) {
                            return
                        }
                        val reason = notice.reason
                        if (reason == Simulator.Reason.PAUSE || reason == Simulator.Reason.BREAKPOINT) {
                            EventQueue.invokeLater {
                                this@RunGoAction.paused(notice.done, reason, notice.error)
                            }
                        } else {
                            EventQueue.invokeLater { this@RunGoAction.stopped(notice.error, reason!!) }
                        }
                        Globals.SIMULATOR.simulatorNoticeHook.unsubscribe(this)
                    }

                }
                Globals.SIMULATOR.simulatorNoticeHook.subscribe(onSimulatorStopListener)

                val breakPoints = this.executePane!!.textSegment.getSortedBreakPointsArray()
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
            JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.")
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
    fun paused(
        done: Boolean,
        pauseReason: Simulator.Reason,
        pe: SimulationError?
    ) {
        // I doubt this can happen (pause when execution finished), but if so treat it
        // as stopped.
        if (done) {
            this.stopped(pe, Simulator.Reason.NORMAL_TERMINATION)
            return
        }
        if (pauseReason == Simulator.Reason.BREAKPOINT) {
            this.mainUI.messagesPane.postMessage(
                this.name + ": execution paused at breakpoint: " + FileStatus.systemFile!!.getName() + "\n\n"
            )
        } else {
            this.mainUI.messagesPane.postMessage(
                this.name + ": execution paused by user: " + FileStatus.systemFile!!.getName() + "\n\n"
            )
        }
        this.mainUI.messagesPane.selectMessageTab()
        this.executePane!!.textSegment.setCodeHighlighting(true)
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
    fun stopped(pe: SimulationError?, reason: Simulator.Reason) {
        // show final register and data segment values.
        this.executePane!!.registerValues.updateRegisters()
        this.executePane!!.fpRegValues.updateRegisters()
        this.executePane!!.csrValues.updateRegisters()
        this.executePane!!.dataSegment.updateValues()
        FileStatus.setSystemState(FileStatus.State.TERMINATED)
        this.mainUI.venusIO.resetFiles() // close any files opened in MIPS program
        // Bring CSRs to the front if terminated due to exception.
        if (pe != null) {
            this.mainUI.registersPane.setSelectedComponent(this.executePane!!.csrValues)
            this.executePane!!.textSegment.setCodeHighlighting(true)
            this.executePane!!.textSegment.unhighlightAllSteps()
            this.executePane!!.textSegment.highlightStepAtAddress(Globals.REGISTER_FILE.programCounter - 4)
        }
        when (reason) {
            Simulator.Reason.NORMAL_TERMINATION -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution completed successfully.\n\n"
                )
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (" + Globals.exitCode + ") --\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
            }

            Simulator.Reason.CLIFF_TERMINATION -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by null instruction.\n\n"
                )
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running (dropped off bottom) --\n\n"
                )
                this.mainUI.messagesPane.selectRunMessageTab()
            }

            Simulator.Reason.EXCEPTION -> {
                this.mainUI.messagesPane.postMessage(
                    pe!!.message.generateReport()
                )
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated with errors.\n\n"
                )
            }

            Simulator.Reason.STOP -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution terminated by user.\n\n"
                )
                this.mainUI.messagesPane.selectMessageTab()
            }

            Simulator.Reason.MAX_STEPS -> {
                this.mainUI.messagesPane.postMessage(
                    "\n" + this.name + ": execution step limit of " + maxSteps + " exceeded.\n\n"
                )
                this.mainUI.messagesPane.selectMessageTab()
            }

            else -> {}
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
        val programArguments = this.executePane!!.textSegment.getProgramArguments()
        if (programArguments == null || programArguments.isEmpty() || !Globals.BOOL_SETTINGS.getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            return
        }
        storeProgramArguments(programArguments)
    }

    companion object {
        val defaultMaxSteps: Int = -1 // "forever", formerly 10000000; // 10 million
        var maxSteps: Int = defaultMaxSteps

        /**
         * Reset max steps limit to default value at termination of a simulated
         * execution.
         */
        @JvmStatic
        fun resetMaxSteps() {
            maxSteps = defaultMaxSteps
        }
    }
}
