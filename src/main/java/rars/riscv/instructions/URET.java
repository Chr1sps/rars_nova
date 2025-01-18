package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

import static rars.Globals.CS_REGISTER_FILE;

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
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {
        final boolean upie = (CS_REGISTER_FILE.getIntValue("ustatus") & 0x10) == 0x10;
        CS_REGISTER_FILE.updateRegisterByName(
            "ustatus",
            CS_REGISTER_FILE.getLongValue("ustatus") & ~0x10L
        ); // Clear UPIE
        if (upie) { // Set UIE to UPIE
            CS_REGISTER_FILE.updateRegisterByName("ustatus", CS_REGISTER_FILE.getLongValue("ustatus") | 0x1L);
        } else {
            CS_REGISTER_FILE.updateRegisterByName(
                "ustatus",
                CS_REGISTER_FILE.getLongValue("ustatus") & ~0x1L
            ); // Clear UIE
        }
        Globals.REGISTER_FILE.setProgramCounter(CS_REGISTER_FILE.getIntValue("uepc"));
    }
}
