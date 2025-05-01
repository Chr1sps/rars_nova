package rars.riscv.hardware

import rars.events.SimulationError
import rars.riscv.BasicInstruction
import rars.riscv.hardware.registerfiles.RegisterFile
import rars.simulator.Simulator

// TODO: add backstepper support
/**
 * Manages the flow of interrupts to the processor
 *
 * Roughly corresponds to PLIC in the spec.
 */
class InterruptController(
    private val simulator: Simulator,
    private val registerFile: RegisterFile
) {
    /** Status for the interrupt state  */
    @get:Synchronized
    var isExternalPending: Boolean = false
        private set
    private var externalValue = 0

    @get:Synchronized
    var isTimerPending: Boolean = false
        private set

    private var timerValue = 0
    private var trapError: SimulationError? = null
    private var trapPC = 0

    @get:Synchronized
    val isTrapPending: Boolean
        get() = trapError != null

    @Synchronized
    fun claimExternal(): Int {
        require(isExternalPending) { "Cannot claim, no external interrupt pending" }
        isExternalPending = false
        return externalValue
    }

    @Synchronized
    fun claimTimer(): Int {
        require(isTimerPending) { "Cannot claim, no timer interrupt pending" }
        isTimerPending = false
        return timerValue
    }

    @Synchronized
    fun claimTrap(): SimulationError {
        requireNotNull(trapError) { "Cannot claim, no trap pending" }
        require(
            trapPC == registerFile.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
        ) { "trapPC doesn't match current pc" }
        return trapError!!.also { trapError = null }
    }

    @Synchronized
    fun reset() {
        isExternalPending = false
        isTimerPending = false
        trapError = null
    }

    @Synchronized
    fun registerExternalInterrupt(value: Int): Boolean = if (isExternalPending) {
        false
    } else {
        externalValue = value
        isExternalPending = true
        simulator.interrupt()
        true
    }

    @Synchronized
    fun registerTimerInterrupt(value: Int): Boolean = if (isTimerPending) {
        false
    } else {
        timerValue = value
        this.isTimerPending = true
        simulator.interrupt()
        true
    }

    @Synchronized
    fun registerSynchronousTrap(
        se: SimulationError,
        pc: Int
    ): Boolean = if (trapError != null) {
        false
    } else {
        trapError = se
        trapPC = pc
        true
    }
}
