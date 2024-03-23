package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

public class FMVDX extends BasicInstruction {
    public FMVDX() {
        super("fmv.d.x f1, t1", "Move float: move bits representing a double from an 64 bit integer register",
                BasicInstructionFormat.I_FORMAT, "1111001 00000 sssss 000 fffff 1010011", true);
    }

    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        FloatingPointRegisterFile.updateRegisterLong(operands[0], RegisterFile.getValueLong(operands[1]));
    }
}