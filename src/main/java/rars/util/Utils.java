package rars.util;

import org.jetbrains.annotations.Contract;
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

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.stream.Stream;

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
        if (e.mode == RoundingMode.MAX) {
            e.mode = RoundingMode.MIN;
        } else if (e.mode == RoundingMode.MIN) {
            e.mode = RoundingMode.MAX;
        }
    }

    public static <T, U> Stream<Pair<T, U>> zip(@NotNull final Stream<T> first, @NotNull final Stream<U> second) {
        final var firstIterator = first.iterator();
        final var secondIterator = second.iterator();
        return Stream.generate(() -> {
            if (firstIterator.hasNext() && secondIterator.hasNext()) {
                return new Pair<>(firstIterator.next(), secondIterator.next());
            }
            //noinspection ReturnOfNull
            return null;
        }).takeWhile(Objects::nonNull);
    }

    @Contract(value = "_ -> param1", pure = true)
    public static <T> T id(final T t) {
        return t;
    }

    /**
     * Returns the color coded as Stringified 32-bit hex with
     * Red in bits 16-23, Green in bits 8-15, Blue in bits 0-7
     * e.g. "0x00FF3366" where Red is FF, Green is 33, Blue is 66.
     * This is used by Settings initialization to avoid direct
     * use of Color class. Long story. DPS 13-May-2010
     *
     * @return String containing hex-coded color second.
     */
    public static @NotNull String getColorAsHexString(final @NotNull Color color) {
        return Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
    }
}
