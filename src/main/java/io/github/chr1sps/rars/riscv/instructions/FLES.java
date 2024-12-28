package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

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
 * <p>FLES class.</p>
 */
public class FLES extends BasicInstruction {
    /**
     * <p>Constructor for FLES.</p>
     */
    public FLES() {
        super("fle.s t1, f1, f2", "Floating Less than or Equals: if f1 <= f2, set t1 to 1, else set t1 to 0",
                BasicInstructionFormat.R_FORMAT, "1010000 ttttt sssss 000 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        final int[] operands = statement.getOperands();
        final Float32 f1 = Floating.getFloat(operands[1]);
        final Float32 f2 = Floating.getFloat(operands[2]);
        final Environment e = new Environment();
        final boolean result = io.github.chr1sps.jsoftfloat.operations.Comparisons.compareSignalingLessThanEqual(f1, f2, e);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], result ? 1 : 0);
    }
}
