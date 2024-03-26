package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>FCVTLUS class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FCVTLUS extends BasicInstruction {
    /**
     * <p>Constructor for FCVTLUS.</p>
     */
    public FCVTLUS() {
        super("fcvt.lu.s t1, f1, dyn",
                "Convert unsigned 64 bit integer from float: Assigns the value of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100000 00011 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float32 in = new Float32(FloatingPointRegisterFile.getValue(operands[1]));
        long out = io.github.chr1sps.jsoftfloat.operations.Conversions.convertToUnsignedLong(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
