package rars.riscv.dump.formats;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class that represents the "hexadecimal text" memory dump format. The output
 * is a text file with one word of memory per line. The word is formatted
 * using hexadecimal characters, e.g. 3F205A39.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
public class HexTextDumpFormat extends AbstractDumpFormat {

    /**
     * Constructor. There is no standard file extension for this format.
     */
    public HexTextDumpFormat() {
        super("Hexadecimal Text", "HexText", "Written as hex characters to text file", null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents in hexadecimal text format. Each line of
     * text contains one memory word written in hexadecimal characters. Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
     *
     * @see AbstractDumpFormat
     */
    @Override
    public void dumpMemoryRange(@NotNull final File file, final int firstAddress, final int lastAddress, @NotNull final Memory memory)
            throws AddressErrorException, IOException {
        try (final PrintStream out = new PrintStream(new FileOutputStream(file))) {
            StringBuilder string;
            for (int address = firstAddress; address <= lastAddress; address += Memory.WORD_LENGTH_BYTES) {
                final Integer temp = memory.getRawWordOrNull(address);
                if (temp == null)
                    break;
                string = new StringBuilder(Integer.toHexString(temp));
                while (string.length() < 8) {
                    string.insert(0, '0');
                }
                out.println(string);
            }
        }
    }

}