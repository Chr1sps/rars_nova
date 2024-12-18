package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.Globals;

import static rars.settings.Settings.OTHER_SETTINGS;

public final class CurrentMemoryConfiguration {
    public static @NotNull MemoryConfiguration currentConfiguration;

    static {
        currentConfiguration = OTHER_SETTINGS.getMemoryConfiguration();
    }

    private CurrentMemoryConfiguration() {
    }

    public static @NotNull MemoryConfiguration get() {
        return currentConfiguration;
    }

    public static boolean set(final @NotNull MemoryConfiguration configuration) {
        if (configuration != currentConfiguration) {
            currentConfiguration = configuration;
            Globals.memory.clear();
            RegisterFile.getRegister("gp").changeResetValue(configuration.globalPointerAddress);
            RegisterFile.getRegister("sp").changeResetValue(configuration.stackPointerAddress);
            RegisterFile.getProgramCounterRegister().changeResetValue(configuration.textBaseAddress);
            RegisterFile.initializeProgramCounter(configuration.textBaseAddress);
            RegisterFile.resetRegisters();
            return true;
        }
        return false;
    }
}
