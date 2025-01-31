package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

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

public final class URET extends BasicInstruction {
    public static final URET INSTANCE = new URET();

    private URET() {
        super(
            "uret", "Return from handling an interrupt or exception (to uepc)",
            BasicInstructionFormat.I_FORMAT, "000000000010 00000 000 00000 1110011"
        );
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {
        final boolean upie = (context.csrRegisterFile.getIntValue("ustatus") & 0x10) == 0x10;
        context.csrRegisterFile.updateRegisterByName(
            "ustatus",
            context.csrRegisterFile.getLongValue("ustatus") & ~0x10L
        ); // Clear UPIE
        if (upie) { // Set UIE to UPIE
            context.csrRegisterFile
                .updateRegisterByName("ustatus", context.csrRegisterFile.getLongValue("ustatus") | 0x1L);
        } else {
            context.csrRegisterFile.updateRegisterByName(
                "ustatus",
                context.csrRegisterFile.getLongValue("ustatus") & ~0x1L
            ); // Clear UIE
        }
        context.registerFile.setProgramCounter(context.csrRegisterFile.getIntValue("uepc"));
    }
}
