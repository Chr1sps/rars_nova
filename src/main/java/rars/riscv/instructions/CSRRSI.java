package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.RegisterFile;

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

public final class CSRRSI extends BasicInstruction {
    public static final CSRRSI INSTANCE = new CSRRSI();

    private CSRRSI() {
        super(
            "csrrsi t0, fcsr, 10",
            "Atomic Read/Set CSR Immediate: read from the CSR into t0 and logical or a constant into the CSR",
            BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 110 fffff 1110011"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        try {
            final long csr = ControlAndStatusRegisterFile.getValueLong(statement.getOperand(1));
            if (statement.getOperand(2) != 0) {
                if (ControlAndStatusRegisterFile.orRegister(statement.getOperand(1), statement.getOperand(2))) {
                    throw new SimulationException(
                        statement, "Attempt to write to read-only CSR",
                        ExceptionReason.ILLEGAL_INSTRUCTION
                    );
                }
            }
            RegisterFile.updateRegister(statement.getOperand(0), csr);
        } catch (final NullPointerException e) {
            throw new SimulationException(
                statement, "Attempt to access unavailable CSR",
                ExceptionReason.ILLEGAL_INSTRUCTION
            );
        }
    }
}
