package io.github.chr1sps.rars.riscv.instructions;

import java.math.BigInteger;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>FCVTDL class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FCVTDL extends BasicInstruction {
    /**
     * <p>Constructor for FCVTDL.</p>
     */
    public FCVTDL() {
        super("fcvt.d.l f1, t1, dyn", "Convert double from long: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00010 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float64 tmp = new Float64(0);
        Float64 converted = io.github.chr1sps.jsoftfloat.operations.Conversions
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValueLong(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}
