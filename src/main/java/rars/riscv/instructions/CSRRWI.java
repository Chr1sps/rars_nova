package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

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

public final class CSRRWI extends BasicInstruction {
    public static final @NotNull CSRRWI INSTANCE = new CSRRWI();

    private CSRRWI() {
        super(
            "csrrwi t0, fcsr, 10",
            "Atomic Read/Write CSR Immediate: read from the CSR into t0 and write a constant into the CSR",
            BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 101 fffff 1110011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        final var csr = Globals.CS_REGISTER_FILE.getLongValue(statement.getOperand(1));
        if (csr == null) {
            throw new SimulationException(
                statement,
                "Attempt to access unavailable CSR",
                ExceptionReason.ILLEGAL_INSTRUCTION
            );
        }
        Globals.CS_REGISTER_FILE.updateRegisterByNumber(statement.getOperand(1), statement.getOperand(2));
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), csr);

    }
}
