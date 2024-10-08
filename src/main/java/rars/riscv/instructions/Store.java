package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
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

/**
 * Base class for all Store instructions
 *
 * @author Benjamin Landers
 * @version June 2017
 */
public abstract class Store extends BasicInstruction {
    /**
     * <p>Constructor for Store.</p>
     *
     * @param usage       a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     */
    public Store(@NotNull final String usage, final String description, final String funct) {
        super(usage, description, BasicInstructionFormat.S_FORMAT,
                "sssssss fffff ttttt " + funct + " sssss 0100011");
    }

    /**
     * <p>Constructor for Store.</p>
     *
     * @param usage       a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     * @param rv64        a boolean
     */
    public Store(@NotNull final String usage, final String description, final String funct, final boolean rv64) {
        super(usage, description, BasicInstructionFormat.S_FORMAT,
                "sssssss fffff ttttt " + funct + " sssss 0100011", rv64);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        operands[1] = (operands[1] << 20) >> 20;
        try {
            store(RegisterFile.getValue(operands[2]) + operands[1], RegisterFile.getValueLong(operands[0]));
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }

    /**
     * <p>store.</p>
     *
     * @param address the address to store to
     * @param value   the second to store
     * @throws AddressErrorException if any.
     */
    protected abstract void store(int address, long value) throws AddressErrorException;
}
