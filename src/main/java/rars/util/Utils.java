package rars.util;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.ExceptionReason;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.riscv.BasicInstruction;
import rars.riscv.SyscallLoader;
import rars.riscv.syscalls.*;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
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

    public static void findAndSimulateSyscall(final int number, final ProgramStatement statement)
        throws SimulationException {
        final var syscall = SyscallLoader.findSyscall(number);
        if (syscall != null) {
            // TODO: find a cleaner way of doing this
            // This was introduced to solve issue #108
            final boolean is_writing =
                syscall instanceof SyscallPrintChar ||
                    syscall instanceof SyscallPrintDouble ||
                    syscall instanceof SyscallPrintFloat ||
                    syscall instanceof SyscallPrintInt ||
                    syscall instanceof SyscallPrintIntBinary ||
                    syscall instanceof SyscallPrintIntHex ||
                    syscall instanceof SyscallPrintIntUnsigned ||
                    syscall instanceof SyscallPrintString ||
                    syscall instanceof SyscallWrite;
            if (!is_writing) {
                SystemIO.flush(true);
            }
            syscall.simulate(statement);
            return;
        }
        throw new SimulationException(
            statement,
            "invalid or unimplemented syscall service: %d ".formatted(number),
            ExceptionReason.ENVIRONMENT_CALL
        );
    }

    public static void processBranch(final int displacement) {
        // Decrement needed because PC has already been incremented
        Globals.REGISTER_FILE.setProgramCounter(Globals.REGISTER_FILE.getProgramCounter() + displacement - BasicInstruction.BASIC_INSTRUCTION_LENGTH);
    }

    public static void processJump(final int targetAddress) {
        Globals.REGISTER_FILE.setProgramCounter(targetAddress);
    }

    /**
     * Method to process storing of a return address in the given
     * register. This is used only by the "and link"
     * instructions: jal and jalr
     * The parameter is register number to receive the return address.
     */
    public static void processReturnAddress(final int register) {
        Globals.REGISTER_FILE.updateRegisterByNumber(register, Globals.REGISTER_FILE.getProgramCounter());
    }

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
            // noinspection ReturnOfNull
            return null;
        }).takeWhile(Objects::nonNull);
    }

    @SafeVarargs
    public static <T> Stream<? extends T> concatStreams(
        final @NotNull Stream<? extends T> first,
        final Stream<? extends T> @NotNull ... others
    ) {
        var result = first;
        for (final var other : others) {
            result = Stream.concat(result, other);
        }
        return result;
    }

    /**
     * Intersperses a separator between each element of the stream.
     *
     * @param stream
     *     The stream to intersperse
     * @param separator
     *     The separator element to insert between each element of the stream
     * @return A new stream with the separator interspersed between each element of the original stream
     */
    public static <T> @NotNull Stream<T> intersperseStream(final @NotNull Stream<T> stream, final T separator) {
        return stream.flatMap(t ->
            Stream.of(separator, t)
        ).skip(1);
    }

    public static <T> @NotNull List<T> intersperseList(final @NotNull List<T> list, final T separator) {
        return intersperseStream(list.stream(), separator).toList();
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
        return BinaryUtils.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
    }

    public static @NotNull Font deriveFontFromStyle(final @NotNull Font baseFont, final @NotNull TokenStyle style) {
        var fontStyle = 0;
        if (style.isBold()) fontStyle |= Font.BOLD;
        if (style.isItalic()) fontStyle |= Font.ITALIC;
        // noinspection MagicConstant
        return baseFont.deriveFont(fontStyle);
    }
}
