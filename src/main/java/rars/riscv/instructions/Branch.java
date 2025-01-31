package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.simulator.SimulationContext;
import rars.util.Utils;

import java.util.Objects;
import java.util.function.BiFunction;

/*
Copyright (c) 2017,  Benjamin Landers

Developed by Benjamin Landers (benjaminrlanders@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Base class for all branching instructions
 * <p>
 * Created mainly for making the branch simulator code simpler, but also does
 * help with code reuse
 *
 * @author Benjamin Landers
 * @version June 2017
 */
public final class Branch extends BasicInstruction {
    public static final @NotNull Branch BEQ = makeBranch(
        "beq",
        "Branch if equal : Branch to statement at label's address if t1 and t2 are equal",
        "000",
        (statement, registerFile) -> Objects.equals(
            registerFile.getLongValue(statement.getOperand(0)), registerFile.getLongValue(statement.getOperand(1))
        )
    );
    public static final @NotNull Branch BGE = makeBranch(
        "bge",
        "Branch if greater than or equal: Branch to statement at label's address if t1 is greater than or equal " +
            "to t2",
        "101",
        (statement, registerFile) -> registerFile.getLongValue(statement.getOperand(0)) >=
            registerFile.getLongValue(statement.getOperand(1))
    );
    public static final @NotNull Branch BGEU = makeBranch(
        "bgeu",
        "Branch if greater than or equal to (unsigned): Branch to statement at label's address if t1 is greater " +
            "than or equal to t2 (with an unsigned interpretation)",
        "111",
        (statement, registerFile) -> Long.compareUnsigned(
            registerFile.getLongValue(statement.getOperand(0)),
            registerFile.getLongValue(statement.getOperand(1))
        ) >= 0
    );
    public static final @NotNull Branch BLT = makeBranch(
        "blt",
        "Branch if less than: Branch to statement at label's address if t1 is less than t2",
        "100",
        (statement, registerFile) -> registerFile.getLongValue(statement.getOperand(0)) < registerFile
            .getLongValue(
                statement.getOperand(1))
    );
    public static final @NotNull Branch BLTU = makeBranch(
        "bltu",
        "Branch if less than (unsigned): Branch to statement at label's address if t1 is less than t2 (with an " +
            "unsigned interpretation)",
        "110",
        (statement, registerFile) -> Long.compareUnsigned(
            registerFile.getLongValue(statement.getOperand(0)),
            registerFile.getLongValue(statement.getOperand(1))
        ) < 0
    );
    public static final @NotNull Branch BNE = makeBranch(
        "bne",
        "Branch if not equal : Branch to statement at label's address if t1 and t2 are not equal",
        "001",
        (statement, registerFile) -> {
            final var firstValue = (int) registerFile.getIntValue(statement.getOperand(0));
            final var secondValue = (int) registerFile.getIntValue(statement.getOperand(1));
            return firstValue != secondValue;
        }
    );
    public final @NotNull BiFunction<@NotNull ProgramStatement, @NotNull RegisterFile, @NotNull Boolean> willBranch;

    private Branch(
        final @NotNull String operand,
        final @NotNull String description,
        final @NotNull String funct,
        final @NotNull BiFunction<@NotNull ProgramStatement, @NotNull RegisterFile, @NotNull Boolean> willBranch
    ) {
        super(
            "%s t1,t2,label".formatted(operand),
            description,
            BasicInstructionFormat.B_FORMAT,
            "ttttttt sssss fffff %s ttttt 1100011 ".formatted(funct)
        );
        this.willBranch = willBranch;
    }

    private static @NotNull Branch makeBranch(
        final @NotNull String usage,
        final @NotNull String description,
        final @NotNull String funct,
        final @NotNull BiFunction<@NotNull ProgramStatement, @NotNull RegisterFile, @NotNull Boolean> willBranchCallback
    ) {
        return new Branch(usage, description, funct, willBranchCallback);
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) {
        if (this.willBranch.apply(statement, context.registerFile)) {
            Utils.processBranch(context.registerFile, statement.getOperand(2), this.getInstructionLength());
        }
    }
}
