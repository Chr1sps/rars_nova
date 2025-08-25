package rars.riscv.hardware.registerFiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.exceptions.SimulationException;
import rars.notices.RegisterAccessNotice;
import rars.riscv.hardware.registers.Register;
import rars.util.BinaryUtils;
import rars.util.ListenerDispatcher;

import java.util.ArrayList;

public abstract class RegisterFileBase {

    protected final @NotNull Register @NotNull [] registers;
    public final char registerNumberPrefix;
    
    private final @NotNull ArrayList<ListenerDispatcher<RegisterAccessNotice>.Handle> registerListeners;
    private final @NotNull ListenerDispatcher<@NotNull RegisterAccessNotice> registerChangeDispatcher;
    public final @NotNull ListenerDispatcher<@NotNull RegisterAccessNotice>.Hook registerChangeHook;

    protected RegisterFileBase(
        final char registerNumberPrefix,
        final @NotNull Register @NotNull [] registers
    ) {
        this.registerNumberPrefix = registerNumberPrefix;
        this.registers = registers;
        this.registerListeners = new ArrayList<>(registers.length);
        this.registerChangeDispatcher = new ListenerDispatcher<>();
        this.registerChangeHook = this.registerChangeDispatcher.getHook();
        for (final var register : this.registers) {
            final var handle = register.registerChangeHook.subscribe(
                this.registerChangeDispatcher::dispatch
            );
            this.registerListeners.add(handle);
        }
    }

    public final @Nullable Long updateRegisterByName(final @NotNull String registerName, final long newValue) throws
        SimulationException {
        final var register = this.getRegisterByName(registerName);
        if (register == null) {
            return null;
        }
        return this.updateRegister(register, newValue);
    }

    protected abstract int convertFromLong(final long value);

    public final long getLongValue(final @NotNull Register register) {
        return register.getValue();
    }

    public final int getIntValue(final @NotNull Register register) {
        return this.convertFromLong(register.getValue());
    }

    public final @Nullable Integer getIntValue(final @NotNull String registerName) {
        final var register = this.getRegisterByName(registerName);
        if (register == null) {
            return null;
        }
        return this.convertFromLong(register.getValue());
    }

    public final @Nullable Integer getIntValue(final int registerNumber) {
        final var register = this.getRegisterByNumber(registerNumber);
        if (register == null) {
            return null;
        }
        return this.convertFromLong(register.getValue());
    }

    public final @Nullable Long getLongValue(final @NotNull String registerName) {
        final var register = this.getRegisterByName(registerName);
        if (register == null) {
            return null;
        }
        return register.getValue();
    }

    public final @Nullable Long getLongValue(final int registerNumber) {
        final var register = this.getRegisterByNumber(registerNumber);
        if (register == null) {
            return null;
        }
        return this.getLongValue(register);
    }

    public final @Nullable Long updateRegisterByNumber(final int registerNumber, final long newValue) throws
        SimulationException {
        final var register = this.getRegisterByNumber(registerNumber);
        if (register == null) {
            return null;
        }
        return this.updateRegister(register, newValue);
    }

    public abstract long updateRegister(final @NotNull Register register, final long newValue) throws
        SimulationException;

    /**
     * Returns all the registers in the register file.
     *
     * @return An array of all the registers in the register file.
     */
    public @NotNull Register @NotNull [] getRegisters() {
        return this.registers;
    }

    public final @Nullable Register getRegisterByNumber(final int registerNumber) {
        for (final var register : this.registers) {
            if (register.number == registerNumber) {
                return register;
            }
        }
        return null;
    }

    public final @Nullable Register getRegisterByName(final @NotNull String name) {
        if (name.length() < 2) {
            return null;
        }

        // Handle a direct name
        for (final var register : this.registers) {
            if (register.name.equals(name)) {
                return register;
            }
        }
        // Handle prefix case
        if (name.charAt(0) == this.registerNumberPrefix) {
            if (name.charAt(1) == 0) { // Ensure that it is a normal decimal number
                if (name.length() > 2) {
                    return null;
                }
                return this.getRegisterByNumber(0);

            }

            final var integerNumber = BinaryUtils.stringToIntFast(name.substring(1));
            if (integerNumber == null) {
                return null;
            }
            return this.getRegisterByNumber(integerNumber);
        }
        return null;
    }

    public void resetRegisters() {
        for (final var register : this.registers) {
            register.resetValue();
        }
    }
}
