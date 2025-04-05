package rars.riscv.dump;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import rars.riscv.dump.formats.IntelHexDumpFormat;
import rars.riscv.dump.formats.SegmentWindowDumpFormat;

import java.util.List;

import static rars.riscv.dump.formats.DumpFormatBaseKt.*;

/**
 * This class provides a list of all dump formats available in RARS.
 */
public final class DumpFormats {
    public static final @NotNull
    @Unmodifiable List<@NotNull DumpFormat> DUMP_FORMATS = List.of(
        ASCII_TEXT_DUMP_FORMAT,
        BINARY_DUMP_FORMAT,
        BINARY_TEXT_DUMP_FORMAT,
        HEX_TEXT_DUMP_FORMAT,
        IntelHexDumpFormat.INSTANCE,
        SegmentWindowDumpFormat.INSTANCE
    );

    private DumpFormats() {
    }

    public static @Nullable DumpFormat findDumpFormatGivenCommandDescriptor(final String formatCommandDescriptor) {
        return DUMP_FORMATS.stream()
            .filter(format -> format.getCommandDescriptor().equals(formatCommandDescriptor))
            .findAny()
            .orElse(null);
    }

}
