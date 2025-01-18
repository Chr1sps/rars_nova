package rars.riscv.hardware.registerFiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.exceptions.SimulationException;
import rars.riscv.hardware.registers.Register;
import rars.settings.OtherSettings;

public final class FloatingPointRegisterFile extends RegisterFileBase {
    public final @NotNull Register ft0, fa0, fa1;

    public FloatingPointRegisterFile() {
        super('f', createRegisters());
        this.ft0 = this.registers[0];
        this.fa0 = this.registers[10];
        this.fa1 = this.registers[11];
    }

    private static @NotNull Register @NotNull [] createRegisters() {
        return new Register[]{
            new Register("ft0", 0, 0),
            new Register("ft1", 1, 0),
            new Register("ft2", 2, 0),
            new Register("ft3", 3, 0),
            new Register("ft4", 4, 0),
            new Register("ft5", 5, 0),
            new Register("ft6", 6, 0),
            new Register("ft7", 7, 0),
            new Register("fs0", 8, 0),
            new Register("fs1", 9, 0),
            new Register("fa0", 10, 0),
            new Register("fa1", 11, 0),
            new Register("fa2", 12, 0),
            new Register("fa3", 13, 0),
            new Register("fa4", 14, 0),
            new Register("fa5", 15, 0),
            new Register("fa6", 16, 0),
            new Register("fa7", 17, 0),
            new Register("fs2", 18, 0),
            new Register("fs3", 19, 0),
            new Register("fs4", 20, 0),
            new Register("fs5", 21, 0),
            new Register("fs6", 22, 0),
            new Register("fs7", 23, 0),
            new Register("fs8", 24, 0),
            new Register("fs9", 25, 0),
            new Register("fs10", 26, 0),
            new Register("fs11", 27, 0),
            new Register("ft8", 28, 0),
            new Register("ft9", 29, 0),
            new Register("ft10", 30, 0),
            new Register("ft11", 31, 0)
        };
    }

    @Override
    public int convertFromLong(final long value) {
        if ((value & 0xFFFFFFFF_00000000L) == 0xFFFFFFFF_00000000L) {
            return (int) value;
        } else {
            return 0x7FC00000;
        }
    }

    @Override
    public long updateRegister(@NotNull final Register register, final long newValue) throws SimulationException {
        final var previousValue = register.setValue(newValue);
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addFloatingPointRestore(
                register.number,
                previousValue
            );
        }
        return previousValue;
    }

    public void updateRegisterByNumberInt(final int registerNumber, final int value) throws SimulationException {
        final var longValue = value | 0xFFFFFFFF_00000000L; // NAN box if used as float
        this.updateRegisterByNumber(registerNumber, longValue);
    }

    public void updateRegisterInt(final @NotNull Register register, final int value) {
        final var longValue = value | 0xFFFFFFFF_00000000L; // NAN box if used as float
        try {
            this.updateRegister(register, longValue);
        } catch (final SimulationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of the FPU register given to the value given.
     *
     * @param registerNumber
     *     Register to set the value of.
     * @param value
     *     The desired float value for the register.
     */
    public void setRegisterToFloat(final int registerNumber, final float value) throws SimulationException {
        final int intValue = Float.floatToIntBits(value);
        this.updateRegisterByNumberInt(registerNumber, intValue);
    }

    public @Nullable Float getFloatFromRegister(final @NotNull String registerName) {
        final var intValue = this.getIntValue(registerName);
        return intValue == null
            ? null
            : Float.intBitsToFloat(intValue);
    }

    public float getFloatFromRegister(final @NotNull Register register) {
        return Float.intBitsToFloat(this.getIntValue(register));
    }
}
