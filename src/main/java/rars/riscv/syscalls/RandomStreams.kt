/**
 * Utilities for managing random number streams in RARS.
 */
package rars.riscv.syscalls

import rars.simulator.SimulationContext
import java.util.*

private val randomStreams = mutableMapOf<Int, Random>()

fun SimulationContext.setRandomStreamSeed(index: Int, seed: Long) {
    val stream = randomStreams[index]
    if (stream != null) {
        stream.setSeed(seed)
    } else {
        randomStreams.put(index, Random(seed))
    }
}

fun SimulationContext.getRandomStream(reg: String): Random {
    val index: Int = registerFile.getIntValue(reg)!!
    var stream = randomStreams[index]
    if (stream == null) {
        stream = Random() // create a non-seeded stream
        randomStreams.put(index, stream)
    }
    return stream
}
