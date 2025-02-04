package rars.util;

import arrow.core.Either;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import rars.exceptions.SimulationError;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.venus.editors.TokenStyle;

import java.awt.*;
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
    public static @NotNull Either<@NotNull SimulationError, Unit> processReturnAddress(
        final int register,
        final @NotNull RegisterFile registerFile
    ) {
        return registerFile.updateRegisterByNumber(register, registerFile.getProgramCounter())
            .map(right -> Unit.INSTANCE);
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
        return BinaryUtilsKt.intToHexStringWithPrefix(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
    }

    public static @NotNull Font deriveFontFromStyle(final @NotNull Font baseFont, final @NotNull TokenStyle style) {
        var fontStyle = 0;
        if (style.isBold()) fontStyle |= Font.BOLD;
        if (style.isItalic()) fontStyle |= Font.ITALIC;
        // noinspection MagicConstant
        return baseFont.deriveFont(fontStyle);
    }
}
