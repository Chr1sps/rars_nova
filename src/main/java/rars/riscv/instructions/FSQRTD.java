package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

/**
 * <p>FSQRTD class.</p>
 */
public final class FSQRTD extends BasicInstruction {
    public static final FSQRTD INSTANCE = new FSQRTD();

    /**
     * <p>Constructor for FSQRTD.</p>
     */
    private FSQRTD() {
        super("fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
                BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float64 result = Arithmetic
                .squareRoot(new Float64(FloatingPointRegisterFile.getValueLong(operands[1])), e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result.bits);
    }
}
