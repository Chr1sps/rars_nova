package io.github.chr1sps.rars.riscv.instructions;

import java.math.BigInteger;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

public class FCVTDLU extends BasicInstruction {
    public FCVTDLU() {
        super("fcvt.d.lu f1, t1, dyn", "Convert double from unsigned long: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00011 sssss ttt fffff 1010011", true);
    }

    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float64 tmp = new Float64(0);
        long value = RegisterFile.getValueLong(operands[1]);
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        Float64 converted = io.github.chr1sps.jsoftfloat.operations.Conversions.convertFromInt(unsigned, e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}
