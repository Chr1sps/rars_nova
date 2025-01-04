package rars.riscv.dump.formats;

import org.jetbrains.annotations.NotNull;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class that represents the "binary" memory dump format. The output
 * is a binary file containing the memory words as a byte stream. Output
 * is produced using PrintStream's write() method.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
public class BinaryDumpFormat extends AbstractDumpFormat {

    public BinaryDumpFormat() {
        super("Binary", "Binary", "Written as byte stream to binary file");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents in pure binary format. One byte at a time
     * using PrintStream's write() method. Adapted by Pete Sanderson from
     * code written by Greg Gibeling.
     *
     * @see AbstractDumpFormat
     */
    @Override
    public void dumpMemoryRange(
            @NotNull final File file,
            final int firstAddress,
            final int lastAddress,
            @NotNull final Memory memory)
            throws AddressErrorException, IOException {
        try (final PrintStream out = new PrintStream(new FileOutputStream(file))) {
            for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                final Integer temp = memory.getRawWordOrNull(address);
                if (temp == null)
                    break;
                final int word = temp;
                for (int i = 0; i < 4; i++)
                    out.write((word >>> (i << 3)) & 0xFF);
            }
        }
    }

}
