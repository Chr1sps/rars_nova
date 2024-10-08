package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for 4 argument floating point instructions
 */
public abstract class FusedDouble extends BasicInstruction {
    /**
     * <p>Constructor for FusedDouble.</p>
     *
     * @param usage       a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param op          a {@link java.lang.String} object
     */
    public FusedDouble(final String usage, final String description, final String op) {
        super(usage + ", dyn", description, BasicInstructionFormat.R4_FORMAT,
                "qqqqq 01 ttttt sssss " + "ppp" + " fffff 100" + op + "11");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[4], statement);
        final Float64 result = compute(new Float64(FloatingPointRegisterFile.getValueLong(operands[1])),
                new Float64(FloatingPointRegisterFile.getValueLong(operands[2])),
                new Float64(FloatingPointRegisterFile.getValueLong(operands[3])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result.bits);
    }

    /**
     * <p>compute.</p>
     *
     * @param r1 The first register
     * @param r2 The second register
     * @param r3 The third register
     * @param e  a {@link Environment} object
     * @return The second to store to the destination
     */
    protected abstract Float64 compute(Float64 r1, Float64 r2, Float64 r3, Environment e);
}
