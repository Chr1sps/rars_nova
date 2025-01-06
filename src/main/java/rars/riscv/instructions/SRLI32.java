package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
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

public final class SRLI32 extends BasicInstruction {
    public static final @NotNull SRLI32 INSTANCE = new SRLI32();

    private SRLI32() {
        super(
            "srli t1,t2,10",
            "Shift right logical : Set t1 to result of shifting t2 right by number of bits specified by immediate",
            BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        // Uses >>> because 0 fill
        final long newValue = RegisterFile.INSTANCE.getIntValue(statement.getOperand(1)) >>> statement.getOperand(2);
        RegisterFile.INSTANCE.updateRegisterByNumber(statement.getOperand(0), newValue);

    }
}
