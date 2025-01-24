package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.InstructionsRegistry;
import rars.simulator.SimulationContext;
import rars.util.ConversionUtils;

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
    protected ImmediateInstruction(
        final @NotNull String usage,
        final @NotNull String description,
        final @NotNull String funct,
        final boolean isRV64
    ) {
        super(
            usage, description, BasicInstructionFormat.I_FORMAT,
            "tttttttttttt sssss %s fffff 001%s011".formatted(funct, isRV64 ? 1 : 0)
        );
    }

    protected ImmediateInstruction(
        final @NotNull String usage,
        final @NotNull String description,
        final @NotNull String funct
    ) {
        this(usage, description, funct, false);
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final var upperImmediate = (statement.getOperand(2) << 20) >> 20;
        final long newValue = (InstructionsRegistry.RV64_MODE_FLAG)
            ? compute(
            context.registerFile().getLongValue(statement.getOperand(1)),
            Integer.valueOf(upperImmediate).longValue()
        ) : computeW(
            context.registerFile().getIntValue(statement.getOperand(1)),
            upperImmediate
        );
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), newValue);
    }

    protected abstract long compute(long value, long immediate);

    protected int computeW(final int value, final int immediate) {
        return ConversionUtils.longLowerHalfToInt(compute(value, immediate));
    }
}
