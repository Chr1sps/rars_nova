package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.notices.RegisterAccessNotice;
import rars.riscv.hardware.registers.Register;
import rars.util.BinaryUtils;

import java.util.function.Consumer;

public abstract class RegisterFileBase {

    protected final char registerNumberPrefix;
    protected final @NotNull Register @NotNull [] registers;

    protected RegisterFileBase(
        final char registerNumberPrefix,
        final @NotNull Register @NotNull [] registers
    ) {
        this.registerNumberPrefix = registerNumberPrefix;
        this.registers = registers;
    }

    public final @Nullable Long updateRegisterByName(final @NotNull String registerName, final long newValue) {
        final var register = this.getRegisterByName(registerName);
        if (register == null) {
            return null;
        }
        return this.updateRegister(register, newValue);
    }

    protected abstract int convertFromLong(final long value);

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
        return register.getValue();
    }

    public final @Nullable Long updateRegisterByNumber(final int registerNumber, final long newValue) {
        final var register = this.getRegisterByNumber(registerNumber);
        if (register == null) {
            return null;
        }
        return this.updateRegister(register, newValue);
    }

    public abstract long updateRegister(final @NotNull Register register, final long newValue);

    public final @NotNull Register @NotNull [] getRegisters() {
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

    public void addRegistersListener(final @NotNull Consumer<? super RegisterAccessNotice> listener) {
        for (final var register : this.registers) {
            register.registerChangeHook.subscribe(listener);
        }
    }

    public void deleteRegistersListener(final @NotNull Consumer<? super RegisterAccessNotice> listener) {
        for (final var register : this.registers) {
            register.registerChangeHook.unsubscribe(listener);
        }
    }
}
