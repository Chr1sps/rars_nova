package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

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

public abstract class FusedFloat extends BasicInstruction {
    /**
     * <p>Constructor for FusedFloat.</p>
     *
     * @param usage
     *     a {@link java.lang.String} object
     * @param description
     *     a {@link java.lang.String} object
     * @param op
     *     a {@link java.lang.String} object
     */
    protected FusedFloat(final String usage, final String description, final String op) {
        super(
            usage + ", dyn", description, BasicInstructionFormat.R4_FORMAT,
            "qqqqq 00 ttttt sssss " + "ppp" + " fffff 100" + op + "11"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(4), statement);
        final Float32 result = compute(
            new Float32(FloatingPointRegisterFile.getValue(statement.getOperand(1))),
            new Float32(FloatingPointRegisterFile.getValue(statement.getOperand(2))),
            new Float32(FloatingPointRegisterFile.getValue(statement.getOperand(3))), e
        );
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(statement.getOperand(0), result.bits);
    }

    /**
     * <p>compute.</p>
     *
     * @param r1
     *     The first register
     * @param r2
     *     The second register
     * @param r3
     *     The third register
     * @param e
     *     a {@link Environment} object
     * @return The second to store to the destination
     */
    protected abstract @NotNull Float32 compute(@NotNull Float32 r1, @NotNull Float32 r2, @NotNull Float32 r3,
                                                @NotNull Environment e);
}
