package rars.riscv.dump.formats;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;
import rars.settings.BoolSetting;
import rars.util.BinaryUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static rars.settings.BoolSettings.BOOL_SETTINGS;

/**
 * Dump memory contents in Segment Window format. Each line of
 * text output resembles the Text Segment Window or Data Segment Window
 * depending on which segment is selected for the dump. Written
 * using PrintStream's println() method. Each line of Text Segment
 * Window represents one word of text segment memory. The line
 * includes (1) address, (2) machine code in hex, (3) basic instruction,
 * (4) source line. Each line of Data Segment Window represents 8
 * words of data segment memory. The line includes address of first
 * word for that line followed by 8 32-bit values.
 * <p>
 * In either case, addresses and values are displayed in decimal or
 * hexadecimal representation according to the corresponding settings.
 *
 * @author Pete Sanderson
 * @version January 2008
 */
public class SegmentWindowDumpFormat extends AbstractDumpFormat {

    public SegmentWindowDumpFormat() {
        super(
            "Text/Data Segment Window", "SegmentWindow",
            " Text Segment Window or Data Segment Window format to text file"
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents in Segment Window format. Each line of
     * text output resembles the Text Segment Window or Data Segment Window
     * depending on which segment is selected for the dump. Written
     * using PrintStream's println() method.
     *
     * @see AbstractDumpFormat
     */
    @Override
    public void dumpMemoryRange(
        final @NotNull File file,
        final int firstAddress,
        final int lastAddress,
        final @NotNull Memory memory
    ) throws AddressErrorException, IOException {

        // TODO: check if these settings work right
        final boolean doDisplayAddressesInHex = BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX);

        // If address in data segment, print in same format as Data Segment Window
        if (Globals.MEMORY_INSTANCE.isAddressInDataSegment(firstAddress)) {
            int offset = 0;
            try (final var outStream = new PrintStream(new FileOutputStream(file))) {
                final var builder = new StringBuilder();
                for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                    if (offset % 8 == 0) {
                        final var formattedAddress = (doDisplayAddressesInHex)
                            ? BinaryUtils.intToHexString(address)
                            : BinaryUtils.unsignedIntToIntString(address);
                        builder.append(formattedAddress).append("    ");
                    }
                    offset++;
                    final var optWord = memory.getRawWordOrNull(address);
                    if (optWord == null) {
                        break;
                    }
                    builder.append((doDisplayAddressesInHex)
                        ? BinaryUtils.intToHexString(optWord)
                        : ("           " + optWord).substring(optWord.toString().length())).append(" ");
                    if (offset % 8 == 0) {
                        outStream.println(builder);
                    }
                }
            }
        } else if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(firstAddress)) {
            // If address in text segment, print in same format as Text Segment Window
            try (final var outStream = new PrintStream(new FileOutputStream(file))) {
                outStream.println("Address     Code        Basic                        Line Source");
                outStream.println();
                for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                    final var builder = new StringBuilder();
                    final var formattedAddress = (doDisplayAddressesInHex)
                        ? BinaryUtils.intToHexString(address)
                        : BinaryUtils.unsignedIntToIntString(address);
                    builder.append(formattedAddress).append("    ");
                    final var optWord = memory.getRawWordOrNull(address);
                    if (optWord == null) {
                        break;
                    }
                    builder.append(BinaryUtils.intToHexString(optWord));
                    builder.append("  ");
                    try {
                        final ProgramStatement ps = memory.getStatement(address);
                        builder.append("%-29s".formatted(ps.getPrintableBasicAssemblyStatement()));
                        if (ps.sourceLine != null) {
                            builder.append("%-5s".formatted(ps.sourceLine.lineNumber()));
                            builder.append(ps.sourceLine.source());
                            builder.append(("%-5s".formatted(Integer.toString(ps.sourceLine.lineNumber()))));
                            builder.append(ps.sourceLine.source());
                        }
                    } catch (final AddressErrorException ignored) {
                    }
                    outStream.println(builder);
                }
            }
        }

    }

}
