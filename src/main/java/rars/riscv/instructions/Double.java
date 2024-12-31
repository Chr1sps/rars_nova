package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public abstract class Double extends BasicInstruction {
    protected Double(final String name, final String description, final String funct) {
        super(
            name + " f1, f2, f3, dyn", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss qqq fffff 1010011"
        );
    }

    protected Double(final String name, final String description, final String funct, final String rm) {
        super(
            name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss " + rm + " fffff 1010011"
        );
    }

    public static @NotNull Float64 getDouble(final int num) {
        return new Float64(FloatingPointRegisterFile.getValueLong(num));
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(3), statement);
        final Float64 result = compute(
            new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(1))),
            new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(2))), e
        );
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(statement.getOperand(0), result.bits);
    }

    public abstract Float64 compute(Float64 f1, Float64 f2, Environment e);
}
