package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.ExceptionReason;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.ControlAndStatusRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

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
 * <p>CSRRCI class.</p>
 *
 */
public class CSRRCI extends BasicInstruction {
    /**
     * <p>Constructor for CSRRCI.</p>
     */
    public CSRRCI() {
        super("csrrci t0, fcsr, 10",
                "Atomic Read/Clear CSR Immediate: read from the CSR into t0 and clear bits of the CSR according to a constant",
                BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 111 fffff 1110011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        try {
            long csr = ControlAndStatusRegisterFile.getValueLong(operands[1]);
            if (operands[2] != 0) {
                if (ControlAndStatusRegisterFile.clearRegister(operands[1], operands[2])) {
                    throw new SimulationException(statement, "Attempt to write to read-only CSR",
                            ExceptionReason.ILLEGAL_INSTRUCTION);
                }
            }
            RegisterFile.updateRegister(operands[0], csr);
        } catch (NullPointerException e) {
            throw new SimulationException(statement, "Attempt to access unavailable CSR",
                    ExceptionReason.ILLEGAL_INSTRUCTION);
        }
    }
}
