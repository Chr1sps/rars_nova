package io.github.chr1sps.rars.riscv.dump;

import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.riscv.hardware.Memory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class that represents the "binary text" memory dump format. The output
 * is a text file with one word of memory per line. The word is formatted
 * using '0' and '1' characters, e.g. 01110101110000011111110101010011.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
public class BinaryTextDumpFormat extends AbstractDumpFormat {

    /**
     * Constructor. There is no standard file extension for this format.
     */
    public BinaryTextDumpFormat() {
        super("Binary Text", "BinaryText", "Written as '0' and '1' characters to text file", null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents in binary text format. Each line of
     * text contains one memory word written as 32 '0' and '1' characters. Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
     *
     * @see AbstractDumpFormat
     */
    public void dumpMemoryRange(File file, int firstAddress, int lastAddress, Memory memory)
            throws AddressErrorException, IOException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            StringBuilder string;
            for (int address = firstAddress; address <= lastAddress; address += Memory.WORD_LENGTH_BYTES) {
                Integer temp = memory.getRawWordOrNull(address);
                if (temp == null)
                    break;
                string = new StringBuilder(Integer.toBinaryString(temp));
                while (string.length() < 32) {
                    string.insert(0, '0');
                }
                out.println(string);
            }
        }
    }

}
