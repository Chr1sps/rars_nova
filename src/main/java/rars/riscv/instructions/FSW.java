package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.Memory;
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

public final class FSW extends BasicInstruction {
    public static final FSW INSTANCE = new FSW();

    private FSW() {
        super("fsw f1, -100(t1)", "Store a float to memory",
            BasicInstructionFormat.S_FORMAT, "sssssss fffff ttttt 010 sssss 0100111");
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        operands[1] = (operands[1] << 20) >> 20;
        try {
            Memory.getInstance().setWord(RegisterFile.getValue(operands[2]) + operands[1],
                (int) FloatingPointRegisterFile.getValueLong(operands[0]));
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
