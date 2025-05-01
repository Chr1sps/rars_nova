package rars.simulator

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.runBlocking
import rars.Globals
import rars.events.*
import rars.io.AbstractIO
import rars.notices.SimulatorNotice
import rars.riscv.BasicInstruction
import rars.riscv.hardware.registerfiles.CSRegisterFile
import rars.settings.OtherSettings.Companion.isBacksteppingEnabled
import rars.util.ListenerDispatcher
import rars.util.toHexStringWithPrefix
import rars.util.unwrap
import rars.venus.run.RunSpeedPanel

open class SimThread(
    private var pc: Int,
    protected val maxSteps: Int,
    breakPoints: IntArray,
    private val io: AbstractIO,
    private val simulatorNoticeDispatcher: ListenerDispatcher<SimulatorNotice>
) : Runnable {
    private val breakPoints: IntArray = breakPoints.sortedArray()

    @Volatile
    private var stop = false

    lateinit var stoppingEvent: StoppingEvent
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
    fun setStop(stoppingEvent: StoppingEvent) {
        require(stoppingEvent is StoppingEvent.UserPaused || stoppingEvent is StoppingEvent.UserStopped)
        this.stoppingEvent = stoppingEvent
        this.stop = true
        (this as Object).notify()
//        lock.withLock {
//            condition.signal()
//        }
    }

    protected open val runSpeed: Double
        get() = RunSpeedPanel.UNLIMITED_SPEED

    private fun startExecution() {
        val notice = SimulatorNotice(
            SimulatorNotice.Action.START,
            maxSteps,
            runSpeed,
            pc,
            event = StoppingEvent.UserStopped, // TODO: this is semantically wrong, replace this
        )
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice)
    }

    // region Stopping event functions

    private fun stopWithError(error: SimulationError) {
        stopExecution(StoppingEvent.ErrorHit(error))
    }

    private fun stopWithBreakpoint() {
        stopExecution(StoppingEvent.BreakpointHit)
    }

    private fun stopWithMaxSteps() {
        stopExecution(StoppingEvent.MaxStepsHit)
    }

    private fun stopWithNormalTermination() {
        stopExecution(StoppingEvent.NormalTermination)
    }

    private fun stopWithCliffTermination() {
        stopExecution(StoppingEvent.CliffTermination)
    }

    private fun stopExecution(
        stoppingEvent: StoppingEvent
//        done: Boolean, reason: Simulator.Reason
    ) {
//        this.done = done
//        this.constructReturnReason = reason
        this.stoppingEvent = stoppingEvent
        this.io.flush()
        val notice = SimulatorNotice(
            SimulatorNotice.Action.STOP,
            this.maxSteps,
            this.runSpeed,
            this.pc,
            stoppingEvent
        )
        // TODO: this is not completely threadsafe, if anything using Swing is observing
        // This can be fixed by making a SwingObserver class that is thread-safe
        this.simulatorNoticeDispatcher.dispatch(notice)
    }

    // endregion Stopping event functions

    private fun handleTrap(se: SimulationError, pc: Int): Boolean {
        assert(se.reason !== EventReason.OTHER) { "Unhandlable exception not thrown through ExitingEception" }
        assert(!se.reason.isInterrupt) { "Interrupts cannot be handled by the trap handler" }

        // set the relevant CSRs
        Globals.CS_REGISTER_FILE.updateRegisterByName(
            "ucause",
            se.reason.value.toLong()
        ).unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName("uepc", pc.toLong())
            .unwrap()
        Globals.CS_REGISTER_FILE.updateRegisterByName(
            "utval",
            se.value.toLong()
        ).unwrap()


        // Get the interrupt handler if it exists
        val utvec = Globals.CS_REGISTER_FILE.getInt("utvec")

        // Mode can be ignored because we are only handling traps
        val base = utvec!! and -0x4

        val exceptionHandler =
            if ((Globals.CS_REGISTER_FILE.getInt("ustatus")!! and 0x1) != 0) {
                // test user-interrupt enable (UIE)
                Globals.MEMORY_INSTANCE.getProgramStatement(base).fold(
                    { null }, { it }
                )
            } else null

        if (exceptionHandler != null) {
            // Set UPIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getInt("ustatus")!!.toLong() or 0x10L
            ).unwrap()
            // Clear UIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE.getLong("ustatus")!! and 0x1L.inv()
            ).unwrap()
            Globals.REGISTER_FILE.programCounter = base
            return true
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            stopWithError(se)
            return false
        }
    }

    private fun handleInterrupt(value: Int, cause: Int, pc: Int): Boolean {
        assert((cause and -0x80000000) != 0) { "Traps cannot be handled by the interupt handler" }
        val code = cause and 0x7FFFFFFF

        // Don't handle cases where that interrupt isn't enabled
        assert(
            (Globals.CS_REGISTER_FILE.getLong("ustatus")!! and 0x1L) != 0L
                && (Globals.CS_REGISTER_FILE.getLong("uie")!! and (1 shl code).toLong()) != 0L
        ) { "The interrupt handler must be enabled" }

        // set the relevant CSRs
        Globals.CS_REGISTER_FILE
            .updateRegisterByName("ucause", cause.toLong())
            .unwrap()
        Globals.CS_REGISTER_FILE
            .updateRegisterByName("uepc", pc.toLong())
            .unwrap()
        Globals.CS_REGISTER_FILE
            .updateRegisterByName("utval", value.toLong())
            .unwrap()

        // Get the interrupt handler if it exists
        val utvec = Globals.CS_REGISTER_FILE.getInt("utvec")!!

        // Handle vectored mode
        val mode = utvec and 0x3
        val base = (utvec and -0x4) + if (mode == 2) 4 * code else 0


        val exceptionHandler = Globals
            .MEMORY_INSTANCE
            .getProgramStatement(base)
            .fold({ null }, { it })
        if (exceptionHandler != null) {
            // Set UPIE
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE
                    .getLong("ustatus")!! or 0x10L
            ).unwrap()
            Globals.CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                Globals.CS_REGISTER_FILE
                    .getLong("ustatus")!! and
                    CSRegisterFile.INTERRUPT_ENABLE.toLong().inv()
            ).unwrap()


//             ControlAndStatusRegisterFile.clearRegister("ustatus", ControlAndStatusRegisterFile.INTERRUPT_ENABLE);
            Globals.REGISTER_FILE.programCounter = base
            return true
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            stopWithError(
                SimulationError.create(
                    "Interrupt handler was not supplied, but interrupt enable was high",
                    EventReason.OTHER
                )
            )
            return false
        }
    }

    override fun run() {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1)
        Thread.yield() // let the main thread run a bit to finish updating the GUI

        startExecution()


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
                val ie = (
                    Globals.CS_REGISTER_FILE.ustatus.valueNoNotify and
                        CSRegisterFile.INTERRUPT_ENABLE.toLong()
                    ) != 0L
                // make sure no interrupts sneak in while we are processing them
                this.pc = Globals.REGISTER_FILE.programCounter
                var pendingExternal =
                    Globals.INTERRUPT_CONTROLLER.isExternalPending
                var pendingTimer = Globals.INTERRUPT_CONTROLLER.isTimerPending
                val pendingTrap = Globals.INTERRUPT_CONTROLLER.isTrapPending
                // This is the explicit (in the spec) order that interrupts should be serviced
                when {
                    ie && pendingExternal && (uie and CSRegisterFile.EXTERNAL_INTERRUPT.toLong()) != 0L -> {
                        if (this.handleInterrupt(
                                Globals.INTERRUPT_CONTROLLER.claimExternal(),
                                EventReason.EXTERNAL_INTERRUPT.value, this.pc
                            )
                        ) {
                            pendingExternal = false
                            uip = uip and 0x100L.inv()
                        } else {
                            return  // if the interrupt can't be handled, but the interrupt enable bit is high,
                            // thats an error
                        }
                    }
                    ie && (uip and 0x1L) != 0L && (uie and CSRegisterFile.SOFTWARE_INTERRUPT.toLong()) != 0L -> {
                        if (this.handleInterrupt(
                                0,
                                EventReason.SOFTWARE_INTERRUPT.value,
                                this.pc
                            )
                        ) {
                            uip = uip and 0x1L.inv()
                        } else {
                            return  // if the interrupt can't be handled, but the interrupt enable bit is high,
                            // thats an error
                        }
                    }
                    ie && pendingTimer && (uie and CSRegisterFile.TIMER_INTERRUPT.toLong()) != 0L -> {
                        if (this.handleInterrupt(
                                Globals.INTERRUPT_CONTROLLER.claimTimer(),
                                EventReason.TIMER_INTERRUPT.value,
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
                    }
                    pendingTrap -> {
                        // if we have a pending trap and aren't handling an interrupt it must
                        // be handled
                        if (!this.handleTrap(
                                Globals.INTERRUPT_CONTROLLER.claimTrap(),
                                this.pc - BasicInstruction.BASIC_INSTRUCTION_LENGTH
                            )
                        ) return
                    }
                }
                uip =
                    uip or ((if (pendingExternal) CSRegisterFile.EXTERNAL_INTERRUPT else 0)
                        or (if (pendingTimer) CSRegisterFile.TIMER_INTERRUPT else 0)).toLong()

                if (uip != Globals.CS_REGISTER_FILE.uip.valueNoNotify) {
                    Globals.CS_REGISTER_FILE.updateRegisterByName("uip", uip)
                        .unwrap()
                }

                // always handle interrupts and traps before quiting
                // Check number of instructions executed. Return if at limit (-1 is no limit).
                if (this.maxSteps > 0) {
                    steps++
                    if (steps > this.maxSteps) {
                        stopWithMaxSteps()
                        return
                    }
                }

                this.pc = Globals.REGISTER_FILE.programCounter
                val eitherStmt =
                    Globals.MEMORY_INSTANCE.getProgramStatement(this.pc)
                val statement = when (eitherStmt) {
                    is Either.Right -> eitherStmt.value
                    is Either.Left -> {
                        val error = eitherStmt.value
                        val tmp =
                            if (error.reason == EventReason.LOAD_ACCESS_FAULT) {
                                SimulationError.create(
                                    "Instruction load access error",
                                    EventReason.INSTRUCTION_ACCESS_FAULT
                                )
                            } else {
                                SimulationError.create(
                                    "Instruction load alignment error",
                                    EventReason.INSTRUCTION_ADDR_MISALIGNED
                                )
                            }
                        if (!Globals.INTERRUPT_CONTROLLER.registerSynchronousTrap(
                                tmp,
                                this.pc
                            )
                        ) {
                            Globals.CS_REGISTER_FILE.updateRegisterByName(
                                "uepc",
                                this.pc.toLong()
                            ).unwrap()
                            stopWithError(tmp)
                            return
                        } else {
                            continue
                        }
                    }
                }
                if (statement == null) {
                    stopWithCliffTermination()
                    return
                }
                val doContinue = either {
                    val instruction = statement.instruction as BasicInstruction?
                    ensureNotNull(instruction) {
                        SimulationError.create(
                            statement,
                            ("undefined instruction (" + statement.binaryStatement.toHexStringWithPrefix()
                                + ")"),
                            EventReason.ILLEGAL_INSTRUCTION
                        )
                    }
                    Globals.REGISTER_FILE.incrementPC(instruction.instructionLength)
                    instruction.run {
                        runBlocking {
                            context.simulate(statement)
                        }
                    }.bind()

                    // IF statement added 7/26/06 (explanation above)
                    if (isBacksteppingEnabled) {
                        Globals.PROGRAM!!.backStepper!!.addDoNothing(this@SimThread.pc)
                    }
                }.fold(
                    { event ->
                        when (event) {
                            is BreakpointEvent -> {
                                // EBREAK needs backstepping support too.
                                if (isBacksteppingEnabled) {
                                    Globals.PROGRAM!!.backStepper!!.addDoNothing(
                                        this.pc
                                    )
                                }
                                ebreak = true
                                false
                            }

                            is WaitEvent -> {
                                if (isBacksteppingEnabled) {
                                    Globals.PROGRAM!!.backStepper!!.addDoNothing(
                                        this.pc
                                    )
                                }
                                waiting = true
                                false
                            }

                            is ExitingEvent -> {
                                stopWithNormalTermination()
                                return
                            }

                            is ExitingError -> {
                                stopWithError(event)
                                return
                            }

                            is SimulationError -> {
                                if (Globals.INTERRUPT_CONTROLLER.registerSynchronousTrap(
                                        event,
                                        this.pc
                                    )
                                ) {
                                    true
                                } else {
                                    stopWithError(event)
                                    return
                                }
                            }
                        }
                    },
                    { false }
                )
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
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(
                Globals.CS_REGISTER_FILE.cycle,
                cycle + 1
            )
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(
                Globals.CS_REGISTER_FILE.instret,
                instret + 1
            )
            Globals.CS_REGISTER_FILE.updateRegisterBackdoor(
                Globals.CS_REGISTER_FILE.time,
                time
            )

            // Return if we've reached a breakpoint.
            if (ebreak || Globals.REGISTER_FILE.programCounter in breakPoints) {
                stopWithBreakpoint()
                return
            }

            // Wait if WFI ran
            if (waiting) {
                if (!(Globals.INTERRUPT_CONTROLLER.isExternalPending || Globals.INTERRUPT_CONTROLLER.isTimerPending)) {
                    synchronized(this) {
                        try {
                            (this as Object).wait()
                        } catch (_: InterruptedException) {
                        }
                    }
//                    lock.withLock {
//                        try {
//                            condition.await()
//                        } catch (_: InterruptedException) {
//                            // Don't bother catching an interruption
//                        }
//                    }
                }
                waiting = false
            }

            this.onEndLoop()
        }
        this.stopExecution(stoppingEvent)
    }

    protected open fun onEndLoop() {
    }

//    /**
//     * Signals the condition variable to wake up any waiting threads.
//     * This method is used by external classes to interrupt the simulation.
//     */
//    fun signalCondition() {
//        lock.withLock {
//            condition.signal()
//        }
//    }
}
