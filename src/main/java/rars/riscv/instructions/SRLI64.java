package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
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

public final class SRLI64 extends BasicInstruction {
    public static final @NotNull SRLI64 INSTANCE = new SRLI64();

    private SRLI64() {
        super(
            "srli t1,t2,33",
            "Shift right logical : Set t1 to result of shifting t2 right by number of bits specified by immediate",
            BasicInstructionFormat.R_FORMAT, "000000 tttttt sssss 101 fffff 0010011"
        );
    }

    @Override
    public void simulate(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {
        // Uses >>> because 0 fill
        final var value = context.registerFile.getLongValue(statement.getOperand(1));
        final var shifted = value >>> statement.getOperand(2);
        context.registerFile.updateRegisterByNumber(statement.getOperand(0), shifted);
    }
}
