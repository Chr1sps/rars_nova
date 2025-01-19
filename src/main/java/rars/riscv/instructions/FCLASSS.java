package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.types.Floating;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;
import rars.riscv.hardware.registerFiles.RegisterFile;

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

public final class FCLASSS extends BasicInstruction {
    public static final FCLASSS INSTANCE = new FCLASSS();

    private FCLASSS() {
        super(
            "fclass.s t1, f1", "Classify a floating point number",
            BasicInstructionFormat.I_FORMAT, "1110000 00000 sssss 001 fffff 1010011"
        );
    }

    /**
     * Sets the one bit in t1 for every number
     * 0 t1 is −infinity.
     * 1 t1 is a negative normal number.
     * 2 t1 is a negative subnormal number.
     * 3 t1 is −0.
     * 4 t1 is +0.
     * 5 t1 is a positive subnormal number.
     * 6 t1 is a positive normal number.
     * 7 t1 is +infinity.
     * 8 t1 is a signaling NaN (Not implemented due to Java).
     * 9 t1 is a quiet NaN.
     */
    public static <T extends Floating<T>> void fclass(
        @NotNull final T in,
        final int out,
        final @NotNull RegisterFile registerFile
    ) throws SimulationException {
        if (in.isNaN()) {
            registerFile.updateRegisterByNumber(out, in.isSignalling() ? 0x100 : 0x200);
        } else {
            final boolean negative = in.isSignMinus();
            if (in.isInfinite()) {
                registerFile.updateRegisterByNumber(out, negative ? 0x001 : 0x080);
            } else if (in.isZero()) {
                registerFile.updateRegisterByNumber(out, negative ? 0x008 : 0x010);
            } else if (in.isSubnormal()) {
                registerFile.updateRegisterByNumber(out, negative ? 0x004 : 0x020);
            } else {
                registerFile.updateRegisterByNumber(out, negative ? 0x002 : 0x040);
            }
        }
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final Float32 in = new Float32(context.fpRegisterFile().getIntValue(statement.getOperand(1)));
        fclass(in, statement.getOperand(0), context.registerFile());
    }
}
