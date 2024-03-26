package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

import java.math.BigInteger;

/**
 * <p>FCVTDW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FCVTDW extends BasicInstruction {
    /**
     * <p>Constructor for FCVTDW.</p>
     */
    public FCVTDW() {
        super("fcvt.d.w f1, t1, dyn", "Convert double from integer: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00000 sssss ttt fffff 1010011");
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
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValue(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}
