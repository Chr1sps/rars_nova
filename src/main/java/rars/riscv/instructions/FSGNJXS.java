package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

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

public final class FSGNJXS extends BasicInstruction {
    public static final @NotNull FSGNJXS INSTANCE = new FSGNJXS();

    private FSGNJXS() {
        super(
            "fsgnjx.s f1, f2, f3",
            "Floating point sign injection (xor):  xor the sign bit of f2 with the sign bit of f3 and assign it to f1",
            BasicInstructionFormat.R_FORMAT,
            "0010000 ttttt sssss 010 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {
        final var f2 = Globals.FP_REGISTER_FILE.getIntValue(statement.getOperand(1));
        final var f3 = Globals.FP_REGISTER_FILE.getIntValue(statement.getOperand(2));
        final var result = (f2 & 0x7FFFFFFF) | ((f2 ^ f3) & 0x80000000);
        Globals.FP_REGISTER_FILE.updateRegisterByNumberInt(statement.getOperand(0), result);
    }
}
