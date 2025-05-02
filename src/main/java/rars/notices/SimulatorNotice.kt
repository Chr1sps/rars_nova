package rars.notices

import rars.simulator.StoppingEvent

/**
 * Object provided to Observers of the Simulator.
 * They are notified at important phases of the runtime simulator,
 * such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */
data class SimulatorNotice(
    @JvmField val action: Action,
    @JvmField val maxSteps: Int,
    @JvmField val runSpeed: Double,
    @JvmField val programCounter: Int,
    @JvmField val event: StoppingEvent,
) {
    enum class Action {
        START,
        STOP
    }
}