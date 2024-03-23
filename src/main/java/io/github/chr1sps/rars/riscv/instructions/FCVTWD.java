package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

public class FCVTWD extends BasicInstruction {
    public FCVTWD() {
        super("fcvt.w.d t1, f1, dyn", "Convert integer from double: Assigns the value of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100001 00000 sssss ttt fffff 1010011");
    }

    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        int out = io.github.chr1sps.jsoftfloat.operations.Conversions.convertToInt(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
