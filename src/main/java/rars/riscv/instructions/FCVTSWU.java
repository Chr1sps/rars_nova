package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import rars.jsoftfloat.operations.Conversions;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

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
 * <p>FCVTSWU class.</p>
 */
public class FCVTSWU extends BasicInstruction {
    /**
     * <p>Constructor for FCVTSWU.</p>
     */
    public FCVTSWU() {
        super("fcvt.s.wu f1, t1, dyn", "Convert float from unsigned integer: Assigns the second of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101000 00001 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float32 tmp = new Float32(0);
        final Float32 converted = Conversions
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValue(operands[1]) & 0xFFFFFFFFL), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], converted.bits);
    }
}
