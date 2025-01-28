package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.Syscall;
import rars.simulator.SimulationContext;

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

public final class ECALL extends BasicInstruction {
    public static final @NotNull ECALL INSTANCE = new ECALL();

    private ECALL() {
        super(
            "ecall", "Issue a system call : Execute the system call specified by value in a7",
            BasicInstructionFormat.I_FORMAT, "000000000000 00000 000 00000 1110011"
        );
    }

    private static void findAndSimulateSyscall(
        final int number,
        final @NotNull ProgramStatement statement,
        final @NotNull SimulationContext context
    ) throws SimulationException {
        final var syscall = Syscall.findSyscall(number);
        if (syscall != null) {
            final boolean isWriting = switch (syscall) {
                case Close,
                     PrintDouble,
                     PrintFloat,
                     PrintInt,
                     PrintIntBinary,
                     PrintIntHex,
                     PrintIntUnsigned,
                     PrintString,
                     Write -> true;
                default -> false;
            };
            if (!isWriting) {
                context.io().flush();
            }
            syscall.simulate(statement, context);
            return;
        }
        throw new SimulationException(
            statement,
            "invalid or unimplemented syscall service: %d ".formatted(number),
            ExceptionReason.ENVIRONMENT_CALL
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        findAndSimulateSyscall(
            context.registerFile().getIntValue("a7"),
            statement,
            context
        );
    }
}
