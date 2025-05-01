package rars.simulator

import rars.events.SimulationError
import rars.io.ConsoleIO
import rars.notices.SimulatorNotice
import rars.util.ListenerDispatcher
import rars.venus.VenusUI

/**
 * Used to simulate the execution of an assembled source program.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
class Simulator {
    private val simulatorNoticeDispatcher =
        ListenerDispatcher<SimulatorNotice>()
    private val stopEventDispatcher = ListenerDispatcher<Unit>()

    @JvmField
    val simulatorNoticeHook = this.simulatorNoticeDispatcher.hook

    @JvmField
    val stopEventHook = this.stopEventDispatcher.hook

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
     * @return a [StoppingEvent] object that indicates how the simulation ended/was stopped
     * @throws SimulationError
     * Throws exception if run-time exception occurs.
     */
    fun simulateCli(
        pc: Int,
        maxSteps: Int,
        consoleIO: ConsoleIO
    ): StoppingEvent = SimThread(
        pc,
        maxSteps,
        IntArray(0),
        consoleIO,
        this.simulatorNoticeDispatcher
    ).run {
        run()
        stoppingEvent
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
        breakPoints: IntArray,
        mainUI: VenusUI
    ) {
        this.simulatorThread = GuiSimThread(
            pc,
            maxSteps,
            breakPoints,
            this.simulatorNoticeDispatcher,
            mainUI
        )
        Thread(this.simulatorThread, "RISCV").start()
    }

    /**
     * Set the volatile stop boolean variable checked by the execution
     * thread at the end of each instruction execution. If the variable
     * is found to be true, the execution thread will depart
     * gracefully so the main thread handling the GUI can take over.
     * This is used by both STOP and PAUSE features.
     */
    private fun interruptExecution(stoppingEvent: StoppingEvent) {
        simulatorThread?.apply {
            setStop(stoppingEvent)
            stopEventDispatcher.dispatch(Unit)
        }
        simulatorThread = null
    }

    fun stopExecution() {
        this.interruptExecution(StoppingEvent.UserStopped)
    }

    fun pauseExecution() {
        this.interruptExecution(StoppingEvent.UserPaused)
    }

    // endregion UI control methods

    fun interrupt() {
        if (this.simulatorThread == null) {
            return
        }
        synchronized(this.simulatorThread!!) {
            (this.simulatorThread as Object).notify()
        }
    }
}
