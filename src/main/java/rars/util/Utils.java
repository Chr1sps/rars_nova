package rars.util;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Stream;

public final class Utils {
    private Utils() {
    }

    public static @NotNull String getStacktraceString(@NotNull final Throwable throwable) {
        final var writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public static void processBranch(
        final @NotNull RegisterFile registerFile,
        final int displacement,
        final int instructionLength
    ) {
        // Decrement needed because PC has already been incremented
        registerFile.setProgramCounter(registerFile.getProgramCounter() + displacement - instructionLength);
    }

    public static void processJump(final int targetAddress, final @NotNull RegisterFile registerFile) {
        registerFile.setProgramCounter(targetAddress);
    }

    /**
     * Method to process storing of a return address in the given
     * register. This is used only by the "and link"
     * instructions: jal and jalr
     * The parameter is register number to receive the return address.
     */
    public static void processReturnAddress(final int register, final @NotNull RegisterFile registerFile) throws
        SimulationException {
        registerFile.updateRegisterByNumber(register, registerFile.getProgramCounter());
    }

    public static void flipRounding(@NotNull final Environment e) {
        if (e.mode == RoundingMode.MAX) {
            e.mode = RoundingMode.MIN;
        } else if (e.mode == RoundingMode.MIN) {
            e.mode = RoundingMode.MAX;
        }
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
