package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.Instructions;
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

/**
 * Base class for all integer instructions using immediates
 *
 * @author Benjamin Landers
 * @version June 2017
 */
public abstract class ImmediateInstruction extends BasicInstruction {
    public ImmediateInstruction(@NotNull final String usage, final @NotNull String description,
                                final @NotNull String funct) {
        super(usage, description, BasicInstructionFormat.I_FORMAT, "tttttttttttt sssss " + funct + " fffff 0010011");
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        if (Instructions.RV64) {
            RegisterFile.updateRegister(
                statement.getOperand(0),
                compute(
                    RegisterFile.getValueLong(statement.getOperand(1)),
                    ((long) statement.getOperand(2) << 20) >> 20
                )
            ); // make sure the immediate is sign-extended
        } else {
            RegisterFile.updateRegister(
                statement.getOperand(0),
                computeW(RegisterFile.getValue(statement.getOperand(1)), (statement.getOperand(2) << 20) >> 20)
            ); // 
            // make sure the immediate is sign-extended
        }
    }

    /**
     * <p>compute.</p>
     *
     * @param value     the second from the register
     * @param immediate the second from the immediate
     * @return the result to be stored from the instruction
     */
    protected abstract long compute(long value, long immediate);

    /**
     * <p>computeW.</p>
     *
     * @param value     the truncated second from the register
     * @param immediate the second from the immediate
     * @return the result to be stored from the instruction
     */
    protected int computeW(final int value, final int immediate) {
        return (int) compute(value, immediate);
    }
}
