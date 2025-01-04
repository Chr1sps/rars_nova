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
import java.util.Objects;

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
        @NotNull final File file, final int firstAddress, final int lastAddress,
        @NotNull final Memory memory
    )
        throws AddressErrorException, IOException {

        final PrintStream out = new PrintStream(new FileOutputStream(file));

        // TODO: check if these settings work right
        final boolean hexAddresses =
            BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX);

        // If address in data segment, print in same format as Data Segment Window
        if (Globals.MEMORY_INSTANCE.isAddressInDataSegment(firstAddress)) {
            final boolean hexValues = BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX);
            int offset = 0;
            StringBuilder string = new StringBuilder();
            try {
                for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                    if (offset % 8 == 0) {
                        string = new StringBuilder((
                            (hexAddresses) ? BinaryUtils.intToHexString(address)
                                : BinaryUtils.unsignedIntToIntString(address)
                        ) + "    ");
                    }
                    offset++;
                    final Integer temp = memory.getRawWordOrNull(address);
                    if (temp == null) {
                        break;
                    }
                    string.append((hexValues)
                        ? BinaryUtils.intToHexString(temp)
                        : ("           " + temp).substring(temp.toString().length())).append(" ");
                    if (offset % 8 == 0) {
                        out.println(string);
                        string = new StringBuilder();
                    }
                }
            } finally {
                out.close();
            }
            return;
        }

        if (!Globals.MEMORY_INSTANCE.isAddressInTextSegment(firstAddress)) {
            return;
        }

        // If address in text segment, print in same format as Text Segment Window
        out.println("Address     Code        Basic                        Line Source");
        out.println();
        String string;
        try {
            for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                string = (
                    (hexAddresses) ? BinaryUtils.intToHexString(address) :
                        BinaryUtils.unsignedIntToIntString(address)
                )
                    + "  ";
                final Integer temp = memory.getRawWordOrNull(address);
                if (temp == null) {
                    break;
                }
                string += BinaryUtils.intToHexString(temp) + "  ";
                try {
                    final ProgramStatement ps = memory.getStatement(address);
                    string += (ps.getPrintableBasicAssemblyStatement() + "                             ").substring(
                        0,
                        29
                    );
                    string += (
                        ((Objects.equals(ps.getSource(), "")) ? "" : Integer.toString(ps.getSourceLine())) +
                            "     "
                    )
                        .substring(0, 5);
                    string += ps.getSource();
                } catch (final AddressErrorException ignored) {
                }
                out.println(string);
            }
        } finally {
            out.close();
        }
    }

}
