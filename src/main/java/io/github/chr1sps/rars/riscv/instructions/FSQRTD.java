package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

/**
 * <p>FSQRTD class.</p>
 *
 */
public class FSQRTD extends BasicInstruction {
    /**
     * <p>Constructor for FSQRTD.</p>
     */
    public FSQRTD() {
        super("fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
                BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float64 result = io.github.chr1sps.jsoftfloat.operations.Arithmetic
                .squareRoot(new Float64(FloatingPointRegisterFile.getValueLong(operands[1])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result.bits);
    }
}
