package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public final class FCVTDS extends BasicInstruction {
    public static final FCVTDS INSTANCE = new FCVTDS();

    private FCVTDS() {
        super(
            "fcvt.d.s f1, f2, dyn", "Convert a float to a double: Assigned the second of f2 to f1",
            BasicInstructionFormat.R4_FORMAT, "0100001 00000 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float32 in = new Float32(FloatingPointRegisterFile.getValue(statement.getOperand(1)));
        Float64 out = new Float64(0);
        out = FCVTSD.convert(in, out, e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(statement.getOperand(0), out.bits);
    }
}
