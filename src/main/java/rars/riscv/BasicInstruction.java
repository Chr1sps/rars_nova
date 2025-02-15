package rars.riscv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.assembler.DataTypes;

/**
 * Class to represent a basic instruction in the MIPS instruction set.
 * Basic instruction means it translates directly to a 32-bit binary machine
 * instruction.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
public abstract non-sealed class BasicInstruction extends Instruction implements SimulationCallback {
    /**
     * Length in bytes of a machine instruction. Currently just 4 because other
     * instruction sizes defined in the specification are nor supported.
     */
    public static final int BASIC_INSTRUCTION_LENGTH = DataTypes.WORD_SIZE;
    private static final int BASIC_INSTRUCTION_LENGTH_BITS = 32;

    private static final Logger LOGGER = LogManager.getLogger(BasicInstruction.class);
    /**
     * Integer with 1's where constants required (0/1 become 1, f/s/t become 0)
     */
    public final int opcodeMask;
    /**
     * Integer matching constants required (0/1 become 0/1, f/s/t become 0)
     */
    public final int opcodeMatch;
    private final @NotNull BasicInstructionFormat instructionFormat;
    private final @NotNull String operationMask;

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
     *     An example usage of the instruction, as a String.
     * @param instrFormat
     *     The format is R, I, I-branch or J.
     * @param operMask
     *     The opcode mask is a 32 character string that contains the
     *     opcode in binary in the appropriate bit positions and
     *     codes for operand positions ('f', 's', 't') in the
     *     remainding positions.
     * @param description
     *     a {@link String} object
     */
    protected BasicInstruction(
        final @NotNull String example,
        final String description,
        final @NotNull BasicInstructionFormat instrFormat,
        final @NotNull String operMask
    ) {
        super(example, description);
        this.instructionFormat = instrFormat;
        this.operationMask = operMask.replaceAll(" ", ""); // squeeze out any/all spaces
        if (this.operationMask.length() != BASIC_INSTRUCTION_LENGTH_BITS) {
            BasicInstruction.LOGGER.warn("{} mask not " + BASIC_INSTRUCTION_LENGTH_BITS + " bits!", example);
        }

        this.opcodeMask = (int) Long.parseLong(
            this.operationMask.replaceAll("[01]", "1")
                .replaceAll("[^01]", "0"), 2
        );
        this.opcodeMatch = (int) Long.parseLong(this.operationMask.replaceAll("[^1]", "0"), 2);
    }

    /**
     * Gets the 32-character operation mask. Each mask position represents a
     * bit position in the 32-bit machine instruction. Operation codes and
     * unused bits are represented in the mask by 1's and 0's. Operand codes
     * are represented by 'f', 's', and 't' for bits occupied by first, second
     * and third operand, respectively.
     *
     * @return The 32 bit mask, as a String
     */
    public @NotNull String getOperationMask() {
        return this.operationMask;
    }

    /**
     * Gets the operand format of the instruction. MIPS defines 3 of these
     * R-format, I-format, and J-format. R-format is all registers. I-format
     * is address formed from register base with immediate offset. J-format
     * is for jump destination addresses. I have added one more:
     * I-branch-format, for branch destination addresses. These are a variation
     * of the I-format in that the computed value is address relative to the
     * Program Counter. All four formats are represented by static objects.
     *
     * @return The machine instruction format, R, I, J or I-branch.
     */
    public @NotNull BasicInstructionFormat getInstructionFormat() {
        return this.instructionFormat;
    }

    @Override
    public int getInstructionLength() {
        return BasicInstruction.BASIC_INSTRUCTION_LENGTH;
    }
}
