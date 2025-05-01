package rars.simulator

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import rars.events.*
import rars.riscv.BasicInstruction
import rars.riscv.hardware.InterruptController
import rars.riscv.hardware.memory.MutableMemory
import rars.riscv.hardware.registerfiles.RegisterFile
import rars.riscv.hardware.registers.updated.Register
import rars.util.rightOr
import rars.util.toHexStringWithPrefix
import rars.util.unreachable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface Simulation {
    /**
     * Runs the simulation up until the next stopping event. Once the execution
     * has stopped fully and will not be resumed, the next call to this method
     * has undefined behavior.
     */
    suspend fun proceed(): StoppingEvent

    /**
     * Signals the condition variable to wake up any waiting threads.
     * This method is used by external classes to interrupt the simulation.
     */
    fun signalCondition()
}

/**
 * Represents an event that stops the program's execution,
 * either temporarily or permanently.
 */
sealed interface StoppingEvent {
    data object BreakpointHit : StoppingEvent
    data object MaxStepsHit : StoppingEvent
    data object NormalTermination : StoppingEvent
    data object CliffTermination : StoppingEvent
    data object UserPaused : StoppingEvent
    data object UserStopped : StoppingEvent
    data class ErrorHit(val error: SimulationError) : StoppingEvent
}

val StoppingEvent.isDone
    get() = when (this) {
        is StoppingEvent.BreakpointHit -> false
        is StoppingEvent.MaxStepsHit -> false
        is StoppingEvent.NormalTermination -> true
        is StoppingEvent.CliffTermination -> true
        is StoppingEvent.ErrorHit -> true
        is StoppingEvent.UserPaused -> true
        is StoppingEvent.UserStopped -> true
    }

class SimulationImpl internal constructor(
    private var pcValue: Int,
    private val breakpoints: IntArray,
    private val maxSteps: Int? = null,
) : Simulation {
    private var steps = 0
    private var ebreak = false
    private var isWaiting = false
    private val context: SimulationContext = TODO("Implement it")
    private var isStopped = false

    // Lock and condition for thread synchronization
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private val memory: MutableMemory<Int>
    private val uip: Register<Long>
    private val uie: Register<Long>
    private val uepc: Register<Long>
    private val ustatus: Register<Long>
    private val cycle: Register<Long>
    private val instret: Register<Long>
    private val time: Register<Long>

    private val interruptController: InterruptController
    private val registerFile: RegisterFile
    private val backstepper: Backstepper
    private val isBacksteppingEnabled: Boolean get() = backstepper.isEnabled
    /*
    Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1)
    Thread.yield() // let the main thread run a bit to finish updating the GUI

    startExecution() // TODO: notifying (but not in this class)
     */

    private val ucause: Register<Long>
    private val utval: Register<Long>
    private val utvec: Register<Long>

    private fun handleInterrupt(
        value: Int,
        cause: Int,
        pc: Int,
    ): Either<SimulationError, Unit> {
        require(cause and Int.MIN_VALUE != 0) {
            "Traps cannot be handled by the interrupt handler. Cause: $cause, PC: $pc"
        }
        val code = cause and Int.MAX_VALUE
        require(ustatus.value and 0x1L != 0L) { "The interrupt handler must be enabled." }

        // set the relevant CSRs
        ucause.value = cause.toLong()
        uepc.value = pc.toLong()
        utval.value = value.toLong()

        // Get the interrupt handler if it exists
        val utvecValue = utvec.value.toInt()

        // Handle vectored mode
        val mode = utvecValue and 0b11
        val base = (utvecValue and 0b11.inv()) + if (mode == 2) 4 * code else 0

        val exceptionHandler = memory
            .getProgramStatement(base)
            .rightOr(null)
        return if (exceptionHandler != null) {
            // Set UPIE
            ustatus.value = ustatus.value or 0x10L
            ustatus.value = ustatus.value and INTERRUPT_ENABLE.inv()

            registerFile.programCounter = base
            Unit.right()
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            SimulationError.create(
                "Interrupt handler was not supplied, but interrupt enable was high.",
                EventReason.OTHER
            ).left()
        }
    }

    private fun handleTrap(
        error: SimulationError,
        pc: Int
    ): Either<SimulationError, Unit> {
        require(error.reason != EventReason.OTHER) { "Unhandleable exception not thrown through ExitingException" }
        require(!error.reason.isInterrupt) { "Interrupts cannot be handled by the trap handler" }

        ucause.value = error.reason.value.toLong()
        uepc.value = pc.toLong()
        utval.value = error.value.toLong()

        val utvecValue = utvec.value.toInt()
        val base = utvecValue and -0x4

        val exceptionHandler = memory
            .getProgramStatement(base)
            .rightOr(null)
            .takeIf { ustatus.value and 0x1L != 0L }
        return if (exceptionHandler != null) {
            // Set UPIE
            ustatus.value = ustatus.value.toInt().toLong() or 0x10L

            // Clear UIE
            ustatus.value = ustatus.value and 0x1L.inv()
            registerFile.programCounter = base
            Unit.right()
        } else {
            // If we don't have an error handler or exceptions are disabled terminate the
            // process
            error.left()
        }
    }

    override fun signalCondition() {
        lock.withLock {
            condition.signal()
        }
    }

    override suspend fun proceed(): StoppingEvent {
        loop@ while (!isStopped) {
            // TODO: locking the memory here (hence the `try`)
            try {
                var uipValue = uip.value
                val uieValue = uie.value
                val areInterruptsEnabled =
                    ustatus.value and INTERRUPT_ENABLE != 0L
                // make sure no interrupts sneak in while we are processing them
                pcValue = registerFile.programCounter
                var isExternalPending = interruptController.isExternalPending
                var isTimerPending = interruptController.isTimerPending
                val isTrapPending = interruptController.isTrapPending
                if (areInterruptsEnabled) {
                    when {
                        isExternalPending && (uieValue and EXTERNAL_INTERRUPT) != 0L -> handleInterrupt(
                            interruptController.claimExternal(),
                            EventReason.EXTERNAL_INTERRUPT.value,
                            pcValue
                        ).fold(
                            ifLeft = { error ->
                                return StoppingEvent.ErrorHit(error)
                            },
                            ifRight = {
                                isExternalPending = false
                                uipValue = uipValue and 0x100L.inv()
                            }
                        )

                        (uipValue and 0x1L) != 0L && (uieValue and SOFTWARE_INTERRUPT) != 0L -> handleInterrupt(
                            value = 0,
                            EventReason.SOFTWARE_INTERRUPT.value,
                            pcValue
                        ).fold(
                            ifLeft = { error ->
                                return StoppingEvent.ErrorHit(
                                    error
                                )
                            },
                            ifRight = { uipValue = uipValue and 0x1L.inv() }
                        )

                        isTimerPending && (uieValue and TIMER_INTERRUPT) != 0L -> handleInterrupt(
                            interruptController.claimTimer(),
                            EventReason.TIMER_INTERRUPT.value,
                            pcValue,
                        ).fold(
                            ifLeft = { error ->
                                return StoppingEvent.ErrorHit(
                                    error
                                )
                            },
                            ifRight = {
                                isTimerPending = false
                                uipValue = uipValue and 0x10L.inv()
                            }
                        )

                    }
                } else if (isTrapPending) {
                    handleTrap(
                        interruptController.claimTrap(),
                        pcValue - BasicInstruction.BASIC_INSTRUCTION_LENGTH
                    ).onLeft { error ->
                        return StoppingEvent.ErrorHit(error)
                    }
                }
                val newUip = uipValue
                    .or(if (isExternalPending) EXTERNAL_INTERRUPT else 0)
                    .or(if (isTimerPending) TIMER_INTERRUPT else 0)
                if (newUip != this.uip.value) {
                    this.uip.value = newUip
                }

                // Always handle interrupts and traps before quitting.
                // Check the number of instructions executed.
                // Return if at the limit.
                maxSteps?.let { limit ->
                    if (steps >= limit) return@proceed StoppingEvent.MaxStepsHit
                    steps++
                }

                pcValue = registerFile.programCounter
                val eitherStmt = memory.getProgramStatement(pcValue)
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
                        if (!interruptController.registerSynchronousTrap(
                                tmp,
                                this.pcValue
                            )
                        ) {
                            uepc.value = pcValue.toLong()
                            return StoppingEvent.ErrorHit(tmp)
                        } else {
                            continue@loop
                        }
                    }
                }

                if (statement == null) return StoppingEvent.CliffTermination
                val doSkipLoopIteration = either {
                    // TODO (IMPORTANT!!): replace ProgramStatement with
                    // machine statement related API.
                    val instruction = statement.instruction as BasicInstruction
                    ensureNotNull(instruction) {
                        SimulationError.create(
                            statement,
                            ("undefined instruction (" + statement.binaryStatement.toHexStringWithPrefix()
                                + ")"),
                            EventReason.ILLEGAL_INSTRUCTION
                        )
                    }
                    registerFile.incrementPC(instruction.instructionLength)
                    instruction.run {
                        context.simulate(statement)
                    }.bind()

                    if (isBacksteppingEnabled) {
                        backstepper.addDoNothing(pcValue)
                    }
                }.fold(
                    ifLeft = { event ->
                        when (event) {
                            is BreakpointEvent -> {
                                // EBREAK needs backstepping support too.
                                if (isBacksteppingEnabled) {
                                    backstepper.addDoNothing(pcValue)
                                }
                                ebreak = true
                                false
                            }

                            is WaitEvent -> {
                                if (isBacksteppingEnabled) {
                                    backstepper.addDoNothing(pcValue)
                                }
                                isWaiting = true
                                false
                            }

                            is ExitingEvent -> {
                                return StoppingEvent.NormalTermination
                            }

                            is ExitingError -> {
                                return StoppingEvent.ErrorHit(event)
                            }

                            is SimulationError -> {
                                if (interruptController.registerSynchronousTrap(
                                        event,
                                        pcValue
                                    )
                                ) {
                                    true
                                } else {
                                    return StoppingEvent.ErrorHit(event)
                                }
                            }
                        }
                    },
                    ifRight = { false }
                )
                if (doSkipLoopIteration) continue@loop
            } finally {
                // This is here for now while migrating from the old simulator.
                // The old one had locking, I need to see whether it's still
                // needed.
            }
            val cycleValue = cycle.value // TODO: no notify
            val instretValue = instret.value // TODO: no notify
            val currentTime = System.currentTimeMillis()
            cycle.value = cycleValue + 1 // TODO: no notify
            instret.value = instretValue + 1 // TODO: no notify
            time.value = currentTime

            // Return if we've reached a breakpoint.
            if (ebreak || registerFile.programCounter in breakpoints) {
                return StoppingEvent.BreakpointHit
            }

            // Wait if WFI ran
            if (isWaiting) {
                if (!(interruptController.isExternalPending || interruptController.isTimerPending)) {
                    lock.withLock {
                        try {
                            condition.await()
                        } catch (_: InterruptedException) {
                        }
                    }
                }
                isWaiting = false
            }
        }
        unreachable()
    }
}

private const val EXTERNAL_INTERRUPT = 0x100L
private const val TIMER_INTERRUPT = 0x10L
private const val SOFTWARE_INTERRUPT = 0x1L
private const val INTERRUPT_ENABLE = 0x1L

fun createSimulation(
    pc: Int,
    breakpoints: IntArray,
): Simulation = SimulationImpl(
    pc,
    breakpoints,
)
