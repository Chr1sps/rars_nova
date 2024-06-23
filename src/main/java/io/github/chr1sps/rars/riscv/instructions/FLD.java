package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FLD class.</p>
 */
public class FLD extends BasicInstruction {
    /**
     * <p>Constructor for FLD.</p>
     */
    public FLD() {
        super("fld f1, -100(t1)", "Load a double from memory",
                BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 011 fffff 0000111");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        operands[1] = (operands[1] << 20) >> 20;
        try {
            long low = Globals.memory.getWord(RegisterFile.getValue(operands[2]) + operands[1]);
            long high = Globals.memory.getWord(RegisterFile.getValue(operands[2]) + operands[1] + 4);
            FloatingPointRegisterFile.updateRegisterLong(operands[0], (high << 32) | (low & 0xFFFFFFFFL));
        } catch (AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
