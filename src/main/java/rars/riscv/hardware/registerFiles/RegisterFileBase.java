package rars.riscv.hardware.registerFiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.exceptions.SimulationException;
import rars.notices.RegisterAccessNotice;
import rars.riscv.hardware.registers.Register;
import rars.util.BinaryUtils;
import rars.util.ListenerDispatcher;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class RegisterFileBase {

    protected final @NotNull Register @NotNull [] registers;
    private final char registerNumberPrefix;

    protected RegisterFileBase(
        final char registerNumberPrefix,
        final @NotNull Register @NotNull [] registers
    ) {
        this.registerNumberPrefix = registerNumberPrefix;
        this.registers = registers;
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

    public @NotNull ListenerDispatcher.Handle<@NotNull RegisterAccessNotice> addRegistersListener(
        final @NotNull Consumer<? super RegisterAccessNotice> listener
    ) {
        final var handles = new ArrayList<ListenerDispatcher.Handle<RegisterAccessNotice>>(registers.length);
        for (final var register : this.registers) {
            final var handle = register.registerChangeHook.subscribe(listener);
            handles.add(handle);
        }

        return ListenerDispatcher.Handle.compound(handles);   
    }

    public void deleteRegistersListener(final @NotNull ListenerDispatcher.Handle<? super RegisterAccessNotice> listener) {
        for (final var register : this.registers) {
            register.registerChangeHook.unsubscribe(listener);
        }
    }
}
