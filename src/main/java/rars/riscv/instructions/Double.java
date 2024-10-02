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
 * <p>Abstract Double class.</p>
 */
public abstract class Double extends BasicInstruction {
    /**
     * <p>Constructor for Double.</p>
     *
     * @param name        a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     */
    protected Double(final String name, final String description, final String funct) {
        super(name + " f1, f2, f3, dyn", description, BasicInstructionFormat.R_FORMAT,
                funct + "ttttt sssss qqq fffff 1010011");
    }

    /**
     * <p>Constructor for Double.</p>
     *
     * @param name        a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param funct       a {@link java.lang.String} object
     * @param rm          a {@link java.lang.String} object
     */
    protected Double(final String name, final String description, final String funct, final String rm) {
        super(name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
                funct + "ttttt sssss " + rm + " fffff 1010011");
    }

    /**
     * <p>getDouble.</p>
     *
     * @param num a int
     * @return a {@link Float64} object
     */
    public static @NotNull Float64 getDouble(final int num) {
        return new Float64(FloatingPointRegisterFile.getValueLong(num));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[3], statement);
        final Float64 result = compute(new Float64(FloatingPointRegisterFile.getValueLong(operands[1])),
                new Float64(FloatingPointRegisterFile.getValueLong(operands[2])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result.bits);
    }

    /**
     * <p>compute.</p>
     *
     * @param f1 a {@link Float64} object
     * @param f2 a {@link Float64} object
     * @param e  a {@link Environment} object
     * @return a {@link Float64} object
     */
    public abstract Float64 compute(Float64 f1, Float64 f2, Environment e);
}
