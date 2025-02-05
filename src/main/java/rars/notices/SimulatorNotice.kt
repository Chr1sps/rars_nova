package rars.notices

import org.jetbrains.annotations.Contract
import rars.exceptions.SimulationError
import rars.simulator.Simulator
import rars.venus.run.RunSpeedPanel

/**
 * Object provided to Observers of the Simulator.
 * They are notified at important phases of the runtime simulator,
 * such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */
class SimulatorNotice(
    @JvmField val action: Action?,
    @JvmField val maxSteps: Int,
    @JvmField val runSpeed: Double,
    @JvmField val programCounter: Int,
    @JvmField val reason: Simulator.Reason?,
    @JvmField val error: SimulationError?,
    @JvmField val done: Boolean
) {
    @Contract(pure = true)
    override fun toString(): String {
        val prefix = if (this.action == Action.START) "START" else "STOP"
        val speed = if (this.runSpeed == RunSpeedPanel.UNLIMITED_SPEED) "unlimited" else "${this.runSpeed} inst/sec"
        return ("$prefix Max Steps ${this.maxSteps} Speed $speed Prog Ctr ${this.programCounter}")
    }

    enum class Action {
        START,
        STOP
    }
}