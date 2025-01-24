package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.jsoftfloat.types.Float32;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile;

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
 * Base class for float to float operations
 *
 * @author Benjamin Landers
 * @version June 2017
 */
public abstract class Floating extends BasicInstruction {
    protected Floating(final String name, final String description, final String funct) {
        super(
            name + " f1, f2, f3, dyn", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss qqq fffff 1010011"
        );
    }

    protected Floating(final String name, final String description, final String funct, final String rm) {
        super(
            name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss " + rm + " fffff 1010011"
        );
    }

    public static void setfflags(final @NotNull CSRegisterFile csRegisterFile, final @NotNull Environment e) throws
        SimulationException {
        final int fflags = (e.inexact ? 1 : 0) +
            (e.underflow ? 2 : 0) +
            (e.overflow ? 4 : 0) +
            (e.divByZero ? 8 : 0) +
            (e.invalid ? 16 : 0);
        if (fflags != 0) {
            csRegisterFile.updateRegisterByName(
                "fflags", csRegisterFile.getLongValue("fflags") | (long) fflags);
        }
    }

    public static @NotNull RoundingMode getRoundingMode(
        final int RM,
        final @NotNull ProgramStatement statement,
        final @NotNull CSRegisterFile csRegisterFile
    ) throws SimulationException {
        int rm = RM;
        final int frm = csRegisterFile.getIntValue("frm");
        if (rm == 7) {
            rm = frm;
        }
        return switch (rm) {
            case 0 -> // RNE
                RoundingMode.EVEN;
            case 1 -> // RTZ
                RoundingMode.ZERO;
            case 2 -> // RDN
                RoundingMode.MIN;
            case 3 -> // RUP
                RoundingMode.MAX;
            case 4 -> // RMM
                RoundingMode.AWAY;
            default -> throw new SimulationException(
                statement,
                "Invalid rounding mode. RM = %d and frm = %d".formatted(RM, frm),
                ExceptionReason.OTHER
            );
        };
    }

    public static @NotNull Float32 getFloat(final @NotNull FloatingPointRegisterFile fpRegisterFile, final int num) {
        return new Float32(fpRegisterFile.getIntValue(num));
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final var environment = new Environment();
        final var hasRoundingMode = statement.hasOperand(3);
        if (hasRoundingMode) {
            environment.mode = Floating.getRoundingMode(statement.getOperand(3), statement, context.csrRegisterFile());
        }
        final Float32 result = this.compute(
            new Float32(context.fpRegisterFile().getIntValue(statement.getOperand(1))),
            new Float32(context.fpRegisterFile().getIntValue(statement.getOperand(2))), environment
        );
        Floating.setfflags(context.csrRegisterFile(), environment);
        context.fpRegisterFile().updateRegisterByNumberInt(statement.getOperand(0), result.bits);
    }

    public abstract Float32 compute(Float32 f1, Float32 f2, Environment e);
}
