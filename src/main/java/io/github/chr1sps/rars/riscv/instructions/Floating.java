package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.Flags;
import io.github.chr1sps.jsoftfloat.RoundingMode;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.ControlAndStatusRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

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
    /**
     * <p>Constructor for Floating.</p>
     *
     * @param name        a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     */
    protected Floating(String name, String description, String funct) {
        super(name + " f1, f2, f3, dyn", description, BasicInstructionFormat.R_FORMAT,
                funct + "ttttt sssss qqq fffff 1010011");
    }

    /**
     * <p>Constructor for Floating.</p>
     *
     * @param name        a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     * @param rm          a {@link java.lang.String} object
     */
    protected Floating(String name, String description, String funct, String rm) {
        super(name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
                funct + "ttttt sssss " + rm + " fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = getRoundingMode(operands[3], statement);
        Float32 result = compute(new Float32(FloatingPointRegisterFile.getValue(operands[1])),
                new Float32(FloatingPointRegisterFile.getValue(operands[2])), e);
        setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], result.bits);
    }

    /**
     * <p>setfflags.</p>
     *
     * @param e a {@link io.github.chr1sps.jsoftfloat.Environment} object
     */
    public static void setfflags(Environment e) {
        int fflags = (e.flags.contains(Flags.inexact) ? 1 : 0) +
                (e.flags.contains(Flags.underflow) ? 2 : 0) +
                (e.flags.contains(Flags.overflow) ? 4 : 0) +
                (e.flags.contains(Flags.divByZero) ? 8 : 0) +
                (e.flags.contains(Flags.invalid) ? 16 : 0);
        if (fflags != 0)
            ControlAndStatusRegisterFile.orRegister("fflags", fflags);
    }

    /**
     * <p>getRoundingMode.</p>
     *
     * @param RM        a int
     * @param statement a {@link io.github.chr1sps.rars.ProgramStatement} object
     * @return a {@link io.github.chr1sps.jsoftfloat.RoundingMode} object
     * @throws SimulationException if any.
     */
    public static RoundingMode getRoundingMode(int RM, ProgramStatement statement) throws SimulationException {
        int rm = RM;
        int frm = ControlAndStatusRegisterFile.getValue("frm");
        if (rm == 7)
            rm = frm;
        switch (rm) {
            case 0: // RNE
                return RoundingMode.even;
            case 1: // RTZ
                return RoundingMode.zero;
            case 2: // RDN
                return RoundingMode.min;
            case 3: // RUP
                return RoundingMode.max;
            case 4: // RMM
                return RoundingMode.away;
            default:
                throw new SimulationException(statement, "Invalid rounding mode. RM = " + RM + " and frm = " + frm);
        }
    }

    /**
     * <p>compute.</p>
     *
     * @param f1 a {@link io.github.chr1sps.jsoftfloat.types.Float32} object
     * @param f2 a {@link io.github.chr1sps.jsoftfloat.types.Float32} object
     * @param e  a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float32} object
     */
    public abstract Float32 compute(Float32 f1, Float32 f2, Environment e);

    /**
     * <p>getFloat.</p>
     *
     * @param num a int
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float32} object
     */
    public static Float32 getFloat(int num) {
        return new Float32(FloatingPointRegisterFile.getValue(num));
    }
}