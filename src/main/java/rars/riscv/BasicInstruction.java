package rars.riscv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

/**
 * Class to represent a basic instruction in the MIPS instruction set.
 * Basic instruction means it translates directly to a 32-bit binary machine
 * instruction.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
public abstract class BasicInstruction extends Instruction {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BasicInstructionFormat instructionFormat;
    private final @NotNull String operationMask;

    private final int opcodeMask; // integer with 1's where constants required (0/1 become 1, f/s/t become 0)
    private final int opcodeMatch; // integer matching constants required (0/1 become 0/1, f/s/t become 0)

    /**
     * BasicInstruction constructor.
     *
     * @param example     An example usage of the instruction, as a String.
     * @param instrFormat The format is R, I, I-branch or J.
     * @param operMask    The opcode mask is a 32 character string that contains the
     *                    opcode in binary in the appropriate bit positions and
     *                    codes for operand positions ('f', 's', 't') in the
     *                    remainding positions.
     * @param description a {@link java.lang.String} object
     */
    /*
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
     */
    public BasicInstruction(final @NotNull String example, final String description, final BasicInstructionFormat instrFormat,
                            final @NotNull String operMask) {
        this.exampleFormat = example;
        this.mnemonic = Instruction.extractOperator(example);
        this.description = description;
        this.instructionFormat = instrFormat;
        this.operationMask = operMask.replaceAll(" ", ""); // squeeze out any/all spaces
        if (this.operationMask.length() != Instruction.INSTRUCTION_LENGTH_BITS) {
            BasicInstruction.LOGGER.warn("{} mask not " + Instruction.INSTRUCTION_LENGTH_BITS + " bits!", example);
        }

        this.opcodeMask = (int) Long.parseLong(this.operationMask.replaceAll("[01]", "1").replaceAll("[^01]", "0"), 2);
        this.opcodeMatch = (int) Long.parseLong(this.operationMask.replaceAll("[^1]", "0"), 2);
    }

    /**
     * <p>Constructor for BasicInstruction.</p>
     *
     * @param example     a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param instrFormat a {@link BasicInstructionFormat} object
     * @param operMask    a {@link java.lang.String} object
     * @param onlyinrv64  a boolean
     */
    public BasicInstruction(final @NotNull String example, final String description, final BasicInstructionFormat instrFormat,
                            final @NotNull String operMask, final boolean onlyinrv64) {
        this(example, description, instrFormat, operMask);
        if (InstructionSet.rv64 != onlyinrv64) {
            throw new NullPointerException("rv64");
        }
    }

    /**
     * Gets the 32-character operation mask. Each mask position represents a
     * bit position in the 32-bit machine instruction. Operation codes and
     * unused bits are represented in the mask by 1's and 0's. Operand codes
     * are represented by 'f', 's', and 't' for bits occupied by first, secon
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
    public BasicInstructionFormat getInstructionFormat() {
        return this.instructionFormat;
    }

    /**
     * <p>Getter for the field <code>opcodeMask</code>.</p>
     *
     * @return a int
     */
    public int getOpcodeMask() {
        return this.opcodeMask;
    }

    /**
     * <p>Getter for the field <code>opcodeMatch</code>.</p>
     *
     * @return a int
     */
    public int getOpcodeMatch() {
        return this.opcodeMatch;
    }

    /**
     * Method to simulate the execution of a specific MIPS basic instruction.
     *
     * @param statement A ProgramStatement representing the MIPS instruction to
     *                  simulate.
     * @throws SimulationException This is a run-time exception generated during
     *                             simulation.
     */
    public abstract void simulate(ProgramStatement statement) throws SimulationException;
}