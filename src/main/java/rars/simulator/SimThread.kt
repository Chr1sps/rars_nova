package rars.simulator

import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import rars.Globals
import rars.ProgramStatement
import rars.exceptions.*
import rars.io.AbstractIO
import rars.notices.SimulatorNotice
import rars.riscv.BasicInstruction
import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.settings.OtherSettings.Companion.getBackSteppingEnabled
import rars.util.ListenerDispatcher
import rars.util.toHexStringWithPrefix
import rars.util.unwrap
import rars.venus.run.RunSpeedPanel
import java.util.*

open class SimThread(
    private var pc: Int,
    @JvmField protected val maxSteps: Int,
    breakPoints: IntArray,
    private val io: AbstractIO,
    private val simulatorNoticeDispatcher: ListenerDispatcher<SimulatorNotice>
) : Runnable {
    private val breakPoints: IntArray = breakPoints.sortedArray()
    private var done = false
    var pe: SimulationError? = null
        private set

    @Volatile
    private var stop = false
    var constructReturnReason: Simulator.Reason? = null
        private set

    /**
     * Sets to "true" the volatile boolean variable that is tested after each
     * instruction is executed. After calling this method, the next test
     * will yield "true" and "construct" will return.
     *
     * @param reason
     * the Reason for stopping (PAUSE or STOP)
     */
    @Synchronized
    fun setStop(reason: Simulator.Reason?) {
        this.stop = true
        this.constructReturnReason = reason
        (this as Object).notify()
    }

    protected open val runSpeed: Double
        get() = RunSpeedPanel.UNLIMITED_SPEED

    private fun startExecution() {
        val notice = SimulatorNotice(
            SimulatorNotice.Action.START,
            this.maxSteps,
            this.runSpeed,
            this.pc, null, this.pe, this.done
        )
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice)
    }

    private fun stopExecution(done: Boolean, reason: Simulator.Reason?) {
        this.done = done
        this.constructReturnReason = reason
        this.io.flush()
        val notice = SimulatorNotice(
            SimulatorNotice.Action.STOP,
            this.maxSteps,
            this.runSpeed,
            this.pc, reason, this.pe, done
        )
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice)
    }

    private fun handleTrap(se: SimulationError, pc: Int): Boolean {
        assert(se.reason !== ExceptionReason.OTHER) { "Unhandlable exception not thrown through ExitingEception" }
        assert(!se.reason.isInterrupt) { "Interrupts cannot be handled by the trap handler" }

        // set the relevant CSRs
        Globals.CS_REGISTER_FILE.updateRegisterByName("ucause", se.reason.value.toLong()).unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName("uepc", pc.toLong()).unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName("utval", se.value.toLong()).unwrap()


        // Get the interrupt handler if it exists
        val utvec = Globals.CS_REGISTER_FILE.getIntValue("utvec")

        // Mode can be ignored because we are only handling traps
        val base = utvec!! and -0x4

        var exceptionHandler: ProgramStatement? = null
        if ((Globals.CS_REGISTER_FILE.getIntValue("ustatus")!! and 0x1) != 0) { // test user-interrupt enable (UIE)
            try {
                exceptionHandler = Globals.MEMORY_INSTANCE.getStatement(base)
            } catch (_: AddressErrorException) {
                // Handled below
            }
        }

        if (exceptionHandler != null) {
            // Set UPIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getIntValue("ustatus")!!.toLong() or 0x10L
            ).unwrap()
            // Clear UIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getLongValue("ustatus")!! and 0x1L.inv()
            ).unwrap()
            Globals.REGISTER_FILE.setProgramCounter(base)
            return true
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            this.pe = se
            this.stopExecution(true, Simulator.Reason.EXCEPTION)
            return false
        }
    }

    private fun handleInterrupt(value: Int, cause: Int, pc: Int): Boolean {
        assert((cause and -0x80000000) != 0) { "Traps cannot be handled by the interupt handler" }
        val code = cause and 0x7FFFFFFF

        // Don't handle cases where that interrupt isn't enabled
        assert(
            (Globals.CS_REGISTER_FILE.getLongValue("ustatus")!! and 0x1L) != 0L
                    && (Globals.CS_REGISTER_FILE.getLongValue("uie")!! and (1 shl code).toLong()) != 0L
        ) { "The interrupt handler must be enabled" }

        // set the relevant CSRs
        Globals.CS_REGISTER_FILE.updateRegisterByName("ucause", cause.toLong()).unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName("uepc", pc.toLong()).unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName("utval", value.toLong()).unwrap()

        // Get the interrupt handler if it exists
        val utvec = Globals.CS_REGISTER_FILE.getIntValue("utvec")!!

        // Handle vectored mode
        var base = utvec and -0x4
        val mode = utvec and 0x3
        if (mode == 2) {
            base += 4 * code
        }

        var exceptionHandler: ProgramStatement? = null
        try {
            exceptionHandler = Globals.MEMORY_INSTANCE.getStatement(base)
        } catch (_: AddressErrorException) {
            // handled below
        }
        if (exceptionHandler != null) {
            // Set UPIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getLongValue("ustatus")!! or 0x10L
            ).unwrap()
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getLongValue("ustatus")!! and
                        CSRegisterFile.INTERRUPT_ENABLE.toLong().inv()
            ).unwrap()


            // ControlAndStatusRegisterFile.clearRegister("ustatus", ControlAndStatusRegisterFile.INTERRUPT_ENABLE);
            Globals.REGISTER_FILE.setProgramCounter(base)
            return true
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            this.pe = SimulationError.create(
                "Interrupt handler was not supplied, but interrupt enable was high",
                ExceptionReason.OTHER
            )
            this.stopExecution(true, Simulator.Reason.EXCEPTION)
            return false
        }
    }

    override fun run() {
        /*
        The next two statements are necessary for GUI to be consistently updated
        before the simulation gets underway. Without them, this happens only
        intermittently,
        with a consequence that some simulations are interruptable using PAUSE/STOP
        and others
        are not (because one or the other or both is not yet enabled).
        */
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1)
        Thread.yield() // let the main thread run a bit to finish updating the GUI

        this.startExecution()

        /*
        ******************* PS addition 26 July 2006 **********************
        A couple statements below were added for the purpose of assuring that when
        "back stepping" is enabled, every instruction will have at least one entry
        on the back-stepping stack. Most instructions will because they write either
        to a register or memory. But "nop" and branches not taken do not. When the
        user is stepping backward through the program, the stack is popped and if
        an instruction has no entry it will be skipped over in the process. This has
        no effect on the correctness of the mechanism but the visual jerkiness when
        instruction highlighting skips such instrutions is disruptive. Current
        solution
        is to add a "do nothing" stack entry for instructions that do no write
        anything.
        To keep this invisible to the "simulate()" method writer, we
        will push such an entry onto the stack here if there is none for this
        instruction
        by the time it has completed simulating. This is done by the IF statement
        just after the call to the simulate method itself. The BackStepper method
        does
        the aforementioned check and decides whether to push or not. The result
        is a a smoother interaction experience. But it comes at the cost of slowing
        simulation speed for flat-out runs, for every instruction executed even
        though very few will require the "do nothing" stack entry. For stepped or
        timed execution the slower execution speed is not noticeable.
        To avoid this cost I tried a different technique: back-fill with "do
        nothings"
        during the backstepping itself when this situation is recognized. Problem
        was in recognizing all possible situations in which the stack contained such
        a "gap". It became a morass of special cases and it seemed every weird test
        case revealed another one. In addition, when a program
        begins with one or more such instructions ("nop" and branches not taken),
        the backstep button is not enabled until a "real" instruction is executed.
        This is noticeable in stepped mode.
        *********************************************************************
        */
        Globals.REGISTER_FILE.initializeProgramCounter(this.pc)
        var steps = 0

        // Volatile variable initialized false but can be set true by the main thread.
        // Used to stop or pause a running program. See stopSimulation() above.
        var ebreak = false
        var waiting = false
        val context = SimulationContext(
            Globals.REGISTER_FILE,
            Globals.FP_REGISTER_FILE,
            Globals.CS_REGISTER_FILE,
            Globals.MEMORY_INSTANCE,
            this.io
        )
        while (!this.stop) {
            // Perform the RISCV instruction in synchronized block. If external threads
            // agree
            // to access memory and registers only through synchronized blocks on same
            // lock variable, then full (albeit heavy-handed) protection of memory and
            // registers is assured. Not as critical for reading from those resources.
            Globals.MEMORY_REGISTERS_LOCK.lock()
            try {
                // Handle pending interupts and traps first
                var uip = Globals.CS_REGISTER_FILE.uip.valueNoNotify
                val uie = Globals.CS_REGISTER_FILE.uie.valueNoNotify
                val ie =
                    (Globals.CS_REGISTER_FILE.ustatus.valueNoNotify and CSRegisterFile.INTERRUPT_ENABLE.toLong()) != 0L
                // make sure no interrupts sneak in while we are processing them
                this.pc = Globals.REGISTER_FILE.programCounter
                var pendingExternal = Globals.INTERRUPT_CONTROLLER.externalPending()
                var pendingTimer = Globals.INTERRUPT_CONTROLLER.timerPending()
                val pendingTrap = Globals.INTERRUPT_CONTROLLER.trapPending()
                // This is the explicit (in the spec) order that interrupts should be serviced
                if (ie && pendingExternal && (uie and CSRegisterFile.EXTERNAL_INTERRUPT.toLong()) != 0L) {
                    if (this.handleInterrupt(
                            Globals.INTERRUPT_CONTROLLER.claimExternal(),
                            ExceptionReason.EXTERNAL_INTERRUPT.value, this.pc
                        )
                    ) {
                        pendingExternal = false
                        uip = uip and 0x100L.inv()
                    } else {
                        return  // if the interrupt can't be handled, but the interrupt enable bit is high,
                        // thats an error
                    }
                } else if (ie && (uip and 0x1L) != 0L && (uie and CSRegisterFile.SOFTWARE_INTERRUPT.toLong()) != 0L) {
                    if (this.handleInterrupt(0, ExceptionReason.SOFTWARE_INTERRUPT.value, this.pc)) {
                        uip = uip and 0x1L.inv()
                    } else {
                        return  // if the interrupt can't be handled, but the interrupt enable bit is high,
                        // thats an error
                    }
                } else if (ie && pendingTimer && (uie and CSRegisterFile.TIMER_INTERRUPT.toLong()) != 0L) {
                    if (this.handleInterrupt(
                            Globals.INTERRUPT_CONTROLLER.claimTimer(),
                            ExceptionReason.TIMER_INTERRUPT.value,
                            this.pc
                        )
                    ) {
                        pendingTimer = false
                        uip = uip and 0x10L.inv()
                    } else {
                        // if the interrupt can't be handled, but the interrupt enable bit is high,
                        // thats an error
                        return
                    }
                } else if (pendingTrap) {
                    // if we have a pending trap and aren't handling an interrupt it must
                    // be handled
                    if (!this.handleTrap(
                            Globals.INTERRUPT_CONTROLLER.claimTrap(),
                            this.pc - BasicInstruction.BASIC_INSTRUCTION_LENGTH
                        )
                    ) return
                }
                uip = uip or ((if (pendingExternal) CSRegisterFile.EXTERNAL_INTERRUPT else 0)
                        or (if (pendingTimer) CSRegisterFile.TIMER_INTERRUPT else 0)).toLong()

                if (uip != Globals.CS_REGISTER_FILE.uip.valueNoNotify) {
                    Globals.CS_REGISTER_FILE.updateRegisterByName("uip", uip).unwrap()
                }

                // always handle interrupts and traps before quiting
                // Check number of instructions executed. Return if at limit (-1 is no limit).
                if (this.maxSteps > 0) {
                    steps++
                    if (steps > this.maxSteps) {
                        this.stopExecution(false, Simulator.Reason.MAX_STEPS)
                        return
                    }
                }

                this.pc = Globals.REGISTER_FILE.programCounter
                // Get instuction
                val statement: ProgramStatement?
                try {
                    statement = Globals.MEMORY_INSTANCE.getStatement(this.pc)
                } catch (e: AddressErrorException) {
                    val tmp = if (e.reason == ExceptionReason.LOAD_ACCESS_FAULT) {
                        SimulationError.create(
                            "Instruction load access error",
                            ExceptionReason.INSTRUCTION_ACCESS_FAULT
                        )
                    } else {
                        SimulationError.create(
                            "Instruction load alignment error",
                            ExceptionReason.INSTRUCTION_ADDR_MISALIGNED
                        )
                    }
                    if (!Globals.INTERRUPT_CONTROLLER.registerSynchronousTrap(tmp, this.pc)) {
                        this.pe = tmp
                        Globals.CS_REGISTER_FILE.updateRegisterByName("uepc", this.pc.toLong()).unwrap()
                        this.stopExecution(true, Simulator.Reason.EXCEPTION)
                        return
                    } else {
                        continue
                    }
                }
                if (statement == null) {
                    this.stopExecution(true, Simulator.Reason.CLIFF_TERMINATION)
                    return
                }
                val doContinue = either {
                    val instruction = statement.instruction as BasicInstruction?
                    ensureNotNull(instruction) {
                        SimulationError.create(
                            statement,
                            ("undefined instruction (" + statement.binaryStatement.toHexStringWithPrefix()
                                    + ")"),
                            ExceptionReason.ILLEGAL_INSTRUCTION
                        )
                    }
                    Globals.REGISTER_FILE.incrementPC(instruction.instructionLength)
                    instruction.run { context.simulate(statement) }.bind()

                    // IF statement added 7/26/06 (explanation above)
                    if (getBackSteppingEnabled()) {
                        Globals.PROGRAM!!.backStepper!!.addDoNothing(this@SimThread.pc)
                    }
                }.fold({ event ->
                    when (event) {
                        is BreakpointEvent -> {
                            // EBREAK needs backstepping support too.
                            if (getBackSteppingEnabled()) {
                                Globals.PROGRAM!!.backStepper!!.addDoNothing(this.pc)
                            }
                            ebreak = true
                            false
                        }

                        is WaitEvent -> {
                            if (getBackSteppingEnabled()) {
                                Globals.PROGRAM!!.backStepper!!.addDoNothing(this.pc)
                            }
                            waiting = true
                            false
                        }

                        is ExitingEvent -> {
                            this.constructReturnReason = Simulator.Reason.NORMAL_TERMINATION
                            this.stopExecution(true, this.constructReturnReason)
                            return
                        }

                        is ExitingError -> {
                            this.constructReturnReason = Simulator.Reason.EXCEPTION
                            this.pe = event
                            this.stopExecution(true, this.constructReturnReason)
                            return
                        }

                        is SimulationError -> {
                            if (Globals.INTERRUPT_CONTROLLER.registerSynchronousTrap(event, this.pc)) {
                                true
                            } else {
                                this.pe = event
                                this.stopExecution(true, Simulator.Reason.EXCEPTION)
                                return
                            }
                        }
                    }
                }, { false })
                if (doContinue) {
                    continue
                }
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock()
            }

            // Update cycle(h) and instret(h)
            val cycle = Globals.CS_REGISTER_FILE.cycle.valueNoNotify
            val instret = Globals.CS_REGISTER_FILE.instret.valueNoNotify
            val time = System.currentTimeMillis()
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(Globals.CS_REGISTER_FILE.cycle, cycle + 1)
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(Globals.CS_REGISTER_FILE.instret, instret + 1)
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(Globals.CS_REGISTER_FILE.time, time)

            // Return if we've reached a breakpoint.
            if (ebreak || Arrays.binarySearch(this.breakPoints, Globals.REGISTER_FILE.programCounter) >= 0) {
                this.stopExecution(false, Simulator.Reason.BREAKPOINT)
                return
            }

            // Wait if WFI ran
            if (waiting) {
                if (!(Globals.INTERRUPT_CONTROLLER.externalPending() || Globals.INTERRUPT_CONTROLLER.timerPending())) {
                    synchronized(this) {
                        try {
                            (this as Object).wait()
                        } catch (_: InterruptedException) {
                            // Don't bother catching an interruption
                        }
                    }
                }
                waiting = false
            }

            this.onEndLoop()
        }
        this.stopExecution(false, this.constructReturnReason)
    }

    protected open fun onEndLoop() {
    }
}

