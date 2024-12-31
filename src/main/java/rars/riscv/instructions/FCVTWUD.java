package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

public final class FCVTWUD extends BasicInstruction {
    public static final FCVTWUD INSTANCE = new FCVTWUD();

    private FCVTWUD() {
        super(
            "fcvt.wu.d t1, f1, dyn", "Convert unsinged integer from double: Assigns the second of f1 (rounded) to t1",
            BasicInstructionFormat.I_FORMAT, "1100001 00001 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(1)));
        final int out = Conversions.convertToUnsignedInt(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(statement.getOperand(0), out);
    }
}
