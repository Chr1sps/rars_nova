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
 * Intel's Hex memory initialization format
 *
 * @author Leo Alterman
 * @version July 2011
 */
public class IntelHexDumpFormat extends AbstractDumpFormat {

    public IntelHexDumpFormat() {
        super("Intel hex format", "HEX", "Written as Intel Hex Memory File");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents according to the Memory Initialization File
     * (MIF) specification.
     *
     * @see AbstractDumpFormat
     */
    @Override
    public void dumpMemoryRange(
        @NotNull final File file,
        final int firstAddress,
        final int lastAddress,
        @NotNull final Memory memory
    )
        throws AddressErrorException, IOException {
        try (final PrintStream out = new PrintStream(new FileOutputStream(file))) {
            for (int address = firstAddress; address <= lastAddress; address += DataTypes.WORD_SIZE) {
                final Integer temp = memory.getRawWordOrNull(address);
                if (temp == null) {
                    break;
                }
                StringBuilder string = new StringBuilder(Integer.toHexString(temp));
                while (string.length() < 8) {
                    string.insert(0, '0');
                }
                final StringBuilder addr = new StringBuilder(Integer.toHexString(address - firstAddress));
                while (addr.length() < 4) {
                    addr.insert(0, '0');
                }
                int tmp_chksum = 0;
                tmp_chksum += 4;
                tmp_chksum += 0xFF & (address - firstAddress);
                tmp_chksum += 0xFF & ((address - firstAddress) >> 8);
                tmp_chksum += 0xFF & temp;
                tmp_chksum += 0xFF & (temp >> 8);
                tmp_chksum += 0xFF & (temp >> 16);
                tmp_chksum += 0xFF & (temp >> 24);
                tmp_chksum = tmp_chksum % 256;
                tmp_chksum = ~tmp_chksum + 1;
                String chksum = Integer.toHexString(0xFF & tmp_chksum);
                if (chksum.length() == 1) {
                    chksum = '0' + chksum;
                }
                final String finalstr = ":04" + addr + "00" + string + chksum;
                out.println(finalstr.toUpperCase());
            }
            out.println(":00000001FF");
        }

    }
}
