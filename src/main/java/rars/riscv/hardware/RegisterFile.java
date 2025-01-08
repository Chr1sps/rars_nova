package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.SymbolTable;
import rars.riscv.hardware.registers.Register;
import rars.settings.BoolSetting;
import rars.settings.OtherSettings;
import rars.util.ConversionUtils;

import static rars.Globals.BOOL_SETTINGS;

public final class RegisterFile extends RegisterFileBase {
    public static final int GLOBAL_POINTER_REGISTER_INDEX = 3;
    public static final int STACK_POINTER_REGISTER_INDEX = 2;
    public final @NotNull Register zero, sp, gp, pc, a0, a1, a2;
    private final @NotNull SymbolTable globalSymbolTable;

    public RegisterFile(
        final @NotNull SymbolTable globalSymbolTable,
        final @NotNull MemoryConfiguration initialMemoryConfiguration
    ) {
        super('x', createRegisters(initialMemoryConfiguration));
        this.globalSymbolTable = globalSymbolTable;
        this.zero = this.registers[0];
        this.sp = this.registers[STACK_POINTER_REGISTER_INDEX];
        this.gp = this.registers[GLOBAL_POINTER_REGISTER_INDEX];
        this.a0 = this.registers[10];
        this.a1 = this.registers[11];
        this.a2 = this.registers[12];
        this.pc = new Register(
            "pc",
            -1,
            Globals.MEMORY_INSTANCE.getMemoryConfiguration().textBaseAddress
        );
    }

    private static @NotNull Register @NotNull [] createRegisters(final @NotNull MemoryConfiguration initialMemoryConfiguration) {
        final var sp = new Register(
            "sp",
            STACK_POINTER_REGISTER_INDEX,
            initialMemoryConfiguration.stackPointerAddress
        );
        final var gp = new Register(
            "gp",
            GLOBAL_POINTER_REGISTER_INDEX,
            initialMemoryConfiguration.globalPointerAddress
        );
        final var a0 = new Register("a0", 10, 0);
        final var a1 = new Register("a1", 11, 0);
        return new Register[]{
            new Register("zero", 0, 0),
            new Register("ra", 1, 0),
            sp,
            gp,
            new Register("tp", 4, 0),
            new Register("t0", 5, 0),
            new Register("t1", 6, 0),
            new Register("t2", 7, 0),
            new Register("s0", 8, 0),
            new Register("s1", 9, 0),
            a0,
            a1,
            new Register("a2", 12, 0),
            new Register("a3", 13, 0),
            new Register("a4", 14, 0),
            new Register("a5", 15, 0),
            new Register("a6", 16, 0),
            new Register("a7", 17, 0),
            new Register("s2", 18, 0),
            new Register("s3", 19, 0),
            new Register("s4", 20, 0),
            new Register("s5", 21, 0),
            new Register("s6", 22, 0),
            new Register("s7", 23, 0),
            new Register("s8", 24, 0),
            new Register("s9", 25, 0),
            new Register("s10", 26, 0),
            new Register("s11", 27, 0),
            new Register("t3", 28, 0),
            new Register("t4", 29, 0),
            new Register("t5", 30, 0),
            new Register("t6", 31, 0)
        };
    }

    @Override
    public int convertFromLong(final long value) {
        return ConversionUtils.longLowerHalfToInt(value);
    }

    @Override
    public long updateRegister(final @NotNull Register register, final long newValue) {
        if (register == this.zero) {
            return 0;
        }
        final var prevValue = register.setValue(newValue);
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addRegisterFileRestore(
                register.number,
                prevValue
            );
        }
        return prevValue;
    }

    /**
     * Method to increment the Program counter in the general case (not a jump or
     * branch). The offset value is here to allow for non-32-bit instructions
     * (like compressed ones).
     */
    public void incrementPC(final int offset) {
        this.pc.setValue(this.pc.getValue() + offset);
    }

    public void initializeProgramCounter(final int value) {
        this.pc.setValue(value);
    }

    public int getProgramCounter() {
        return (int) this.pc.getValue();
    }

    public int setProgramCounter(final int value) {
        return (int) this.updateRegister(this.pc, value);
    }

    public void initializeProgramCounter(final boolean startAtMain) {
        final int mainAddr = this.globalSymbolTable.getAddress(SymbolTable.getStartLabel());
        final var useMainAddr = startAtMain && mainAddr != SymbolTable.NOT_FOUND && Globals.MEMORY_INSTANCE.isAddressInTextSegment(
            mainAddr);
        final var programCounterValue = useMainAddr ? mainAddr : (int) this.pc.getResetValue();
        this.initializeProgramCounter(programCounterValue);
    }

    @Override
    public void resetRegisters() {
        final var startAtMain = BOOL_SETTINGS.getSetting(BoolSetting.START_AT_MAIN);
        final int mainAddr = this.globalSymbolTable.getAddress(SymbolTable.getStartLabel());
        final var useMainAddr = startAtMain && mainAddr != SymbolTable.NOT_FOUND && Globals.MEMORY_INSTANCE.isAddressInTextSegment(
            mainAddr);
        final var programCounterValue = useMainAddr ? mainAddr : (int) this.pc.getResetValue();
        this.resetRegisters(programCounterValue);
    }

    public void resetRegisters(final int programCounterValue) {
        super.resetRegisters();
        this.initializeProgramCounter(programCounterValue);
    }

    public void setValuesFromConfiguration(final @NotNull MemoryConfiguration configuration) {
        this.gp.changeResetValue(configuration.globalPointerAddress);
        this.sp.changeResetValue(configuration.stackPointerAddress);
        this.pc.changeResetValue(configuration.textBaseAddress);
        this.pc.setValue(configuration.textBaseAddress);
        this.resetRegisters();
    }
}
