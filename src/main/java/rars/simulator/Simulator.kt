package rars.simulator

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.jetbrains.annotations.NotNull
import rars.exceptions.SimulationError
import rars.io.ConsoleIO
import rars.notices.SimulatorNotice
import rars.util.ListenerDispatcher
import rars.venus.VenusUI

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
 * Used to simulate the execution of an assembled source program.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
class Simulator {
    private val simulatorNoticeDispatcher = ListenerDispatcher<@NotNull SimulatorNotice>()
    private val stopEventDispatcher = ListenerDispatcher<@NotNull Unit>()

    @JvmField
    val simulatorNoticeHook = this.simulatorNoticeDispatcher.getHook()

    @JvmField
    val stopEventHook = this.stopEventDispatcher.getHook()

    private var simulatorThread: SimThread? = null

    /**
     * Simulate execution of given source program (in this thread). It must have
     * already been assembled.
     *
     * @param pc
     * address of first instruction to simulate; this goes into
     * program counter
     * @param maxSteps
     * maximum number of steps to perform before returning false
     * (0 or less means no max)
     * @return a [Reason] object that indicates how the simulation ended/was stopped
     * @throws SimulationError
     * Throws exception if run-time exception occurs.
     */
    fun simulateCli(
        pc: Int,
        maxSteps: Int,
        consoleIO: ConsoleIO
    ): Either<SimulationError, Reason> {
        this.simulatorThread = SimThread(
            pc,
            maxSteps,
            IntArray(0),
            consoleIO,
            this.simulatorNoticeDispatcher
        )
        this.simulatorThread!!.run() // Just call run, this is a blocking method
        val pe = this.simulatorThread!!.pe
        val out = this.simulatorThread!!.constructReturnReason!!
        this.simulatorThread = null
        return pe?.left() ?: out.right()
    }

    // region UI control methods
    /**
     * Start simulated execution of given source program (in a new thread). It must
     * have already been assembled.
     *
     * @param pc
     * address of first instruction to simulate; this goes into
     * program counter
     * @param maxSteps
     * maximum number of steps to perform before returning false
     * (0 or less means no max)
     * @param breakPoints
     * array of breakpoint program counter values, use null if
     * none
     */
    fun startSimulation(
        pc: Int,
        maxSteps: Int,
        breakPoints: IntArray?,
        mainUI: VenusUI
    ) {
        this.simulatorThread = GuiSimThread(pc, maxSteps, breakPoints, this.simulatorNoticeDispatcher, mainUI)
        Thread(this.simulatorThread, "RISCV").start()
    }

    /**
     * Set the volatile stop boolean variable checked by the execution
     * thread at the end of each instruction execution. If variable
     * is found to be true, the execution thread will depart
     * gracefully so the main thread handling the GUI can take over.
     * This is used by both STOP and PAUSE features.
     */
    private fun interruptExecution(reason: Reason) {
        if (this.simulatorThread != null) {
            this.simulatorThread!!.setStop(reason)
            this.stopEventDispatcher.dispatch(null)
            this.simulatorThread = null
        }
    }

    fun stopExecution() {
        this.interruptExecution(Reason.STOP)
    }

    fun pauseExecution() {
        this.interruptExecution(Reason.PAUSE)
    }

    // endregion UI control methods
    /**
     *
     * interrupt.
     */
    fun interrupt() {
        if (this.simulatorThread == null) {
            return
        }
        synchronized(this.simulatorThread!!) {
            (this.simulatorThread as Object).notify()
        }
    }

    /**
     * various reasons for simulate to end...
     */
    enum class Reason {
        BREAKPOINT,
        EXCEPTION,
        MAX_STEPS,  // includes step mode (where maxSteps is 1)
        NORMAL_TERMINATION,
        CLIFF_TERMINATION,  // run off bottom of program
        PAUSE,
        STOP
    }
}
