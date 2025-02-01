package rars.riscv

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.assembler.DataTypes

/**
 * Class to represent a basic instruction in the MIPS instruction set.
 * Basic instruction means it translates directly to a 32-bit binary machine
 * instruction.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
abstract class BasicInstruction protected constructor(
    example: String,
    description: String,
    /**
     * The operand format of the instruction. MIPS defines 3 of these
     * R-format, I-format, and J-format. R-format is all registers. I-format
     * is address formed from register base with immediate offset. J-format
     * is for jump destination addresses. I have added one more:
     * I-branch-format, for branch destination addresses. These are a variation
     * of the I-format in that the computed value is address relative to the
     * Program Counter. All four formats are represented by static objects.
     */
    @JvmField val instructionFormat: BasicInstructionFormat,
    operMask: String
) : Instruction(example, description), SimulationCallback {
    /**
     * Integer with 1's where constants required (0/1 become 1, f/s/t become 0)
     */
    val opcodeMask: Int

    /**
     * Integer matching constants required (0/1 become 0/1, f/s/t become 0)
     */
    val opcodeMatch: Int

    /**
     * The 32-character operation mask. Each mask position represents a
     * bit position in the 32-bit machine instruction. Operation codes and
     * unused bits are represented in the mask by 1's and 0's. Operand codes
     * are represented by 'f', 's', and 't' for bits occupied by first, second
     * and third operand, respectively.
     */
    @JvmField
    val operationMask: String = operMask.replace(" ".toRegex(), "") // squeeze out any/all spaces

    /**
     * BasicInstruction constructor.
     * codes for operand positions are:
     * f == First operand
     * s == Second operand
     * t == Third operand
     * example: "add rd,rs,rt" is R format with fields in this order: opcode, rs,
     * rt, rd, shamt, funct.
     * Its opcode is 0, shamt is 0, funct is 0x40. Based on operand order, its mask
     * is
     * "000000ssssstttttfffff00000100000", split into
     * opcode | rs | rt | rd | shamt | funct
     * 000000 | sssss | ttttt | fffff | 00000 | 100000
     * This mask can be used at code generation time to map the assembly component
     * to its
     * correct bit positions in the binary machine instruction.
     * It can also be used at runtime to match a binary machine instruction to the
     * correct
     * instruction simulator -- it needs to match all and only the 0's and 1's.
     *
     * @param example
     * An example usage of the instruction, as a String.
     * @param instructionFormat
     * The format is R, I, I-branch or J.
     * @param operMask
     * The opcode mask is a 32 character string that contains the
     * opcode in binary in the appropriate bit positions and
     * codes for operand positions ('f', 's', 't') in the
     * remainding positions.
     * @param description
     * a [String] object
     */
    init {
        if (this.operationMask.length != BASIC_INSTRUCTION_LENGTH_BITS) {
            LOGGER.warn("{} mask not $BASIC_INSTRUCTION_LENGTH_BITS bits!", example)
        }

        this.opcodeMask = this.operationMask.replace("[01]".toRegex(), "1")
            .replace("[^01]".toRegex(), "0").toLong(
                2
            ).toInt()
        this.opcodeMatch = this.operationMask.replace("[^1]".toRegex(), "0").toLong(2).toInt()
    }

    override val instructionLength = BASIC_INSTRUCTION_LENGTH

    companion object {
        /**
         * Length in bytes of a machine instruction. Currently just 4 because other
         * instruction sizes defined in the specification are nor supported.
         */
        const val BASIC_INSTRUCTION_LENGTH: Int = DataTypes.WORD_SIZE
        private const val BASIC_INSTRUCTION_LENGTH_BITS = 32

        private val LOGGER: Logger = LogManager.getLogger(BasicInstruction::class.java)
    }
}
