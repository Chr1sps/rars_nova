package io.github.chr1sps.rars.riscv.instructions;

import java.math.BigInteger;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>FCVTSL class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FCVTSL extends BasicInstruction {
    /**
     * <p>Constructor for FCVTSL.</p>
     */
    public FCVTSL() {
        super("fcvt.s.l f1, t1, dyn", "Convert float from long: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101000 00010 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float32 tmp = new Float32(0);
        Float32 converted = io.github.chr1sps.jsoftfloat.operations.Conversions
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValueLong(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], converted.bits);
    }
}
