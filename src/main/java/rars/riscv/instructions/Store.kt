package rars.riscv.instructions

import arrow.core.Either
import rars.ProgramStatement
import rars.events.MemoryError
import rars.events.SimulationError
import rars.events.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.memory.Memory
import rars.simulator.SimulationContext
import rars.util.ignoreOk

/**
 * Base class for all Store instructions
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Store(
    usage: String,
    description: String,
    funct: String,
    private val store: (Int, Long, Memory) -> Either<MemoryError, Unit>
) : BasicInstruction(
    usage,
    description,
    BasicInstructionFormat.S_FORMAT,
    "sssssss fffff ttttt $funct sssss 0100011"
) {
    override suspend fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> {
        val upperImmediate: Int = (statement.getOperand(1) shl 20) shr 20
        return store(
            registerFile.getInt(statement.getOperand(2))!! + upperImmediate,
            registerFile.getLong(statement.getOperand(0))!!,
            memory
        ).mapLeft { error ->
            SimulationError.create(statement, error)
        }
    }

    companion object {
        private fun store(
            usage: String,
            description: String,
            funct: String,
            store: (Int, Long, Memory) -> Either<MemoryError, Unit>
        ) = Store(usage, description, funct, store)

        val SB = store(
            "sb t1, -100(t2)",
            "Store byte : Store the low-order 8 bits of t1 into the effective memory byte address",
            "000"
        ) { address, value, memory ->
            memory.setByte(
                address,
                value.and(0xFFL).toByte()
            ).ignoreOk()
        }

        val SH = store(
            "sh t1, -100(t2)",
            "Store halfword : Store the low-order 16 bits of t1 into the effective memory halfword address",
            "001"
        ) { address, value, memory ->
            memory.setHalf(
                address,
                value.and(0xFFFFL).toShort()
            ).ignoreOk()
        }

        val SW = store(
            "sw t1, -100(t2)",
            "Store word : Store contents of t1 into effective memory word address",
            "010"
        ) { address, value, memory ->
            memory.setWord(address, value.toInt()).ignoreOk()
        }

        val SD = store(
            "sd t1, -100(t2)",
            "Store double word : Store contents of t1 into effective memory double word address",
            "011"
        ) { address, value, memory ->
            memory.setDoubleWord(address, value).ignoreOk()
        }
    }
}