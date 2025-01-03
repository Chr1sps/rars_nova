package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public abstract class FusedDouble extends BasicInstruction {
    /**
     * <p>Constructor for FusedDouble.</p>
     *
     * @param usage
     *     a {@link java.lang.String} object
     * @param description
     *     a {@link java.lang.String} object
     * @param op
     *     a {@link java.lang.String} object
     */
    public FusedDouble(final String usage, final String description, final String op) {
        super(
            usage + ", dyn", description, BasicInstructionFormat.R4_FORMAT,
            "qqqqq 01 ttttt sssss " + "ppp" + " fffff 100" + op + "11"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(4), statement);
        final Float64 result = compute(
            new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(1))),
            new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(2))),
            new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(3))), e
        );
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(statement.getOperand(0), result.bits);
    }

    /**
     * <p>compute.</p>
     *
     * @param r1
     *     The first register
     * @param r2
     *     The second register
     * @param r3
     *     The third register
     * @param e
     *     a {@link Environment} object
     * @return The second to store to the destination
     */
    protected abstract Float64 compute(Float64 r1, Float64 r2, Float64 r3, Environment e);
}
