package rars.util;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.registers.Register;

public final class RegisterUtils {
    private RegisterUtils() {
    }

    /**
     * Gets the second of a normal, floating-point or control and status register.
     *
     * @param name
     *     Either the common usage (t0, a0, ft0), explicit numbering (x2,
     *     x3, f0), or CSR name (ustatus)
     * @return The second of the register as an int (floats are encoded as IEEE-754)
     * @throws NullPointerException
     *     if name is invalid; only needs to be checked if
     *     code accesses arbitrary names
     */
    public static int getRegisterValue(final @NotNull String name) {
        Register r = Globals.REGISTER_FILE.getRegisterByName(name);
        if (r == null) {
            r = Globals.FP_REGISTER_FILE.getRegisterByName(name);
        }
        if (r == null) {
            return ControlAndStatusRegisterFile.getValue(name);
        } else {
            return (int) r.getValue();
        }
    }
}
