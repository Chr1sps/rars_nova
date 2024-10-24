package rars.util;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.riscv.AbstractSyscall;
import rars.riscv.BasicInstruction;
import rars.riscv.SyscallLoader;
import rars.riscv.hardware.RegisterFile;
import rars.riscv.syscalls.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Utils {
    private Utils() {
    }

    public static @NotNull String getStacktraceString(@NotNull final Throwable throwable) {
        final var writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }


    /**
     * <p>findAndSimulateSyscall.</p>
     *
     * @param number    a int
     * @param statement a {@link ProgramStatement} object
     * @throws SimulationException if any.
     */
    public static void findAndSimulateSyscall(final int number, final ProgramStatement statement)
            throws SimulationException {
        final AbstractSyscall service = SyscallLoader.findSyscall(number);
        if (service != null) {
            // TODO: find a cleaner way of doing this
            // This was introduced to solve issue #108
            final boolean is_writing = service instanceof SyscallPrintChar ||
                    service instanceof SyscallPrintDouble ||
                    service instanceof SyscallPrintFloat ||
                    service instanceof SyscallPrintInt ||
                    service instanceof SyscallPrintIntBinary ||
                    service instanceof SyscallPrintIntHex ||
                    service instanceof SyscallPrintIntUnsigned ||
                    service instanceof SyscallPrintString ||
                    service instanceof SyscallWrite;
            if (!is_writing) {
                SystemIO.flush(true);
            }
            service.simulate(statement);
            return;
        }
        throw new SimulationException(statement,
                "invalid or unimplemented syscall service: " +
                        number + " ",
                ExceptionReason.ENVIRONMENT_CALL);
    }

    /**
     * <p>processBranch.</p>
     *
     * @param displacement a int
     */
    public static void processBranch(final int displacement) {
        // Decrement needed because PC has already been incremented
        RegisterFile
                .setProgramCounter(RegisterFile.getProgramCounter() + displacement - BasicInstruction.BASIC_INSTRUCTION_LENGTH);
    }

    /**
     * <p>processJump.</p>
     *
     * @param targetAddress a int
     */
    public static void processJump(final int targetAddress) {
        RegisterFile.setProgramCounter(targetAddress);
    }

    /**
     * Method to process storing of a return address in the given
     * register. This is used only by the "and link"
     * instructions: jal and jalr
     * The parameter is register number to receive the return address.
     */
    public static void processReturnAddress(final int register) {
        RegisterFile.updateRegister(register, RegisterFile.getProgramCounter());
    }

    /**
     * <p>flipRounding.</p>
     *
     * @param e a {@link Environment} object
     */
    public static void flipRounding(@NotNull final Environment e) {
        if (e.mode == RoundingMode.max) {
            e.mode = RoundingMode.min;
        } else if (e.mode == RoundingMode.min) {
            e.mode = RoundingMode.max;
        }
    }

}
