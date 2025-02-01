package rars.riscv.hardware.registerFiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.riscv.hardware.registers.LinkedRegister;
import rars.riscv.hardware.registers.MaskedRegister;
import rars.riscv.hardware.registers.ReadOnlyRegister;
import rars.riscv.hardware.registers.Register;
import rars.settings.OtherSettings;

public final class CSRegisterFile extends RegisterFileBase {
    public static final int EXTERNAL_INTERRUPT = 0x100;
    public static final int TIMER_INTERRUPT = 0x10;
    public static final int SOFTWARE_INTERRUPT = 0x1;
    public static final int INTERRUPT_ENABLE = 0x1;

    public final @NotNull Register ustatus, fflags, frm, fcsr,
        uie, utvec, uscratch, uepc, ucause, utval, uip,
        cycle, time, instret, cycleh, timeh, instreth;

    public CSRegisterFile() {
        super('_', createRegisters());
        // Ugly code, but this is the limitation of Java
        // preventing code before the constructor call.
        this.ustatus = this.registers[0];
        this.fflags = this.registers[1];
        this.frm = this.registers[2];
        this.fcsr = this.registers[3];
        this.uie = this.registers[4];
        this.utvec = this.registers[5];
        this.uscratch = this.registers[6];
        this.uepc = this.registers[7];
        this.ucause = this.registers[8];
        this.utval = this.registers[9];
        this.uip = this.registers[10];
        this.cycle = this.registers[11];
        this.time = this.registers[12];
        this.instret = this.registers[13];
        this.cycleh = this.registers[14];
        this.timeh = this.registers[15];
        this.instreth = this.registers[16];
    }

    private static @NotNull Register @NotNull [] createRegisters() {

        final var fcsr = new MaskedRegister("fcsr", 0x003, 0, ~0xFF);

        final var fflags = new LinkedRegister("fflags", 0x001, fcsr, 0x1F);
        final var frm = new LinkedRegister("frm", 0x002, fcsr, 0xE0);

        final var cycle = new ReadOnlyRegister("cycle", 0xC00, 0);
        final var time = new ReadOnlyRegister("time", 0xC01, 0);
        final var instret = new ReadOnlyRegister("instret", 0xC02, 0);

        final var cycleh = new LinkedRegister("cycleh", 0xC80, cycle, 0xFFFFFFFF_00000000L);
        final var timeh = new LinkedRegister("timeh", 0xC81, time, 0xFFFFFFFF_00000000L);
        final var instreth = new LinkedRegister("instreth", 0xC82, instret, 0xFFFFFFFF_00000000L);

        return new Register[]{
            new MaskedRegister("ustatus", 0x000, 0, ~0x11),
            fflags,
            frm,
            fcsr,
            new Register("uie", 0x004, 0),
            new Register("utvec", 0x005, 0),
            new Register("uscratch", 0x040, 0),
            new Register("uepc", 0x041, 0),
            new Register("ucause", 0x042, 0),
            new Register("utval", 0x043, 0),
            new Register("uip", 0x044, 0),
            cycle,
            time,
            instret,
            cycleh,
            timeh,
            instreth,
        };
    }

    @Override
    protected int convertFromLong(final long value) {
        return (int) value;
    }

    @Override
    public long updateRegister(final @NotNull Register register, final long newValue) throws SimulationException {
        if (register instanceof ReadOnlyRegister || register == cycleh || register == timeh || register == instreth) {
            throw new SimulationException("Attempt to write to read-only CSR", ExceptionReason.ILLEGAL_INSTRUCTION);
        }
        final var previousValue = register.setValue(newValue);
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addControlAndStatusRestore(register.number, previousValue);
        }
        return previousValue;
    }

    public long updateRegisterBackdoor(final @NotNull Register register, final long newValue) {
        final var previousValue = register.setValueNoNotify(newValue);
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addControlAndStatusBackdoor(
                register.number,
                previousValue
            );
        }
        return previousValue;
    }

    public @Nullable Long updateRegisterBackdoorByNumber(final int registerNumber, final long newValue) {
        final var register = this.getRegisterByNumber(registerNumber);
        if (register == null) {
            return null;
        }
        return this.updateRegisterBackdoor(register, newValue);
    }

    public @Nullable Long getLongValueNoNotifyByName(final @NotNull String name) {
        final var register = this.getRegisterByName(name);
        if (register == null) {
            return null;
        }
        return register.getValueNoNotify();
    }
}
