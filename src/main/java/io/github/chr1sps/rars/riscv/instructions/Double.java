package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
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
    protected Double(String name, String description, String funct) {
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
    protected Double(String name, String description, String funct, String rm) {
        super(name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
                funct + "ttttt sssss " + rm + " fffff 1010011");
    }

    /**
     * <p>getDouble.</p>
     *
     * @param num a int
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public static @NotNull Float64 getDouble(int num) {
        return new Float64(FloatingPointRegisterFile.getValueLong(num));
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[3], statement);
        Float64 result = compute(new Float64(FloatingPointRegisterFile.getValueLong(operands[1])),
                new Float64(FloatingPointRegisterFile.getValueLong(operands[2])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result.bits);
    }

    /**
     * <p>compute.</p>
     *
     * @param f1 a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     * @param f2 a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     * @param e  a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public abstract Float64 compute(Float64 f1, Float64 f2, Environment e);
}
