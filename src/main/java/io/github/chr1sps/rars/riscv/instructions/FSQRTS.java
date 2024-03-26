package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

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
 * <p>FSQRTS class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FSQRTS extends BasicInstruction {
    /**
     * <p>Constructor for FSQRTS.</p>
     */
    public FSQRTS() {
        super("fsqrt.s f1, f2, dyn", "Floating SQuare RooT: Assigns f1 to the square root of f2",
                BasicInstructionFormat.I_FORMAT, "0101100 00000 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float32 result = io.github.chr1sps.jsoftfloat.operations.Arithmetic
                .squareRoot(new Float32(FloatingPointRegisterFile.getValue(operands[1])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], result.bits);
    }
}
