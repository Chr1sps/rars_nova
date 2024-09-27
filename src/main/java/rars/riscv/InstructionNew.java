package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.assembler.TokenList;

public sealed interface InstructionNew extends SimulationCallback permits ExtendedInstructionNew {

    /**
     * Get operation mnemonic
     *
     * @return operation mnemonic (e.g. addi, sw)
     */
    @NotNull String getName();

    /**
     * Get length in bytes that this instruction requires in its binary form.
     * Default is 4 (holds for all basic instructions), but can be overridden
     * in subclass.
     *
     * @return int length in bytes of corresponding binary instruction(s).
     */
    int getInstructionLength();

    /**
     * Get TokenList corresponding to correct instruction syntax.
     * For example, the instruction with format "sw x1, 100(x2)" yields token list
     * operator:register_number:integer:left_paren:register_number:right_parent
     *
     * @return TokenList object representing correct instruction usage.
     */
    @NotNull TokenList getTokenList();

    /**
     * Get string describing the instruction. This is not used internally by
     * RARS, but is for display to the user.
     *
     * @return String describing the instruction.
     */
    @NotNull String getDescription();

    /**
     * Get string descriptor of instruction's format. This is an example MIPS
     * assembler instruction usage which contains the operator and all operands.
     * Operands are separated by commas, an operand that is the standard name or
     * machine name for a register represents a register, and an integer operand
     * represents an immediate value or address. Here are two examples:
     * "or x1,x2,x3" and "sw x1,100(x2)"
     *
     * @return String representing example instruction format.
     */
    @NotNull String getExampleFormat();
}
