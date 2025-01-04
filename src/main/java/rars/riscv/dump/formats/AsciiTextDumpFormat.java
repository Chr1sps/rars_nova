package rars.riscv.dump.formats;

import org.jetbrains.annotations.NotNull;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;
import rars.util.BinaryUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class that represents the "ASCII text" memory dump format. Memory contents
 * are interpreted as ASCII codes. The output
 * is a text file with one word of memory per line. The word is formatted
 * to leave three spaces for each character. Non-printing characters
 * rendered as period (.) as placeholder. Common escaped characters
 * rendered using backslash and single-character descriptor, e.g. \t for tab.
 *
 * @author Pete Sanderson
 * @version December 2010
 */
public class AsciiTextDumpFormat extends AbstractDumpFormat {

    public AsciiTextDumpFormat() {
        super("ASCII Text", "AsciiText", "Memory contents interpreted as ASCII characters");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Interpret memory contents as ASCII characters. Each line of
     * text contains one memory word written in ASCII characters. Those
     * corresponding to tab, newline, null, etc are rendered as backslash
     * followed by single-character code, e.g. \t for tab, \0 for null.
     * Non-printing character (control code,
     * values above 127) is rendered as a period (.). Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
     *
     * @see AbstractDumpFormat
     */
    @Override
    public void dumpMemoryRange(
        @NotNull final File file, final int firstAddress, final int lastAddress,
        @NotNull final Memory memory
    )
        throws AddressErrorException, IOException {
        try (final PrintStream out = new PrintStream(new FileOutputStream(file))) {
            for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                final Integer temp = memory.getRawWordOrNull(address);
                if (temp == null) {
                    break;
                }
                out.println(BinaryUtils.intToAscii(temp));
            }
        }
    }

}
