package rars.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

import javax.swing.*;
import java.awt.*;

public class BitmapDisplay extends JFrame {
    /*
    Some notes:
    1. The display should have three parameters:
        - base address to the necessary data
        - display width
        - display height.
    2. Should the display block the simulation until the user closes it?
    3. Maybe add support for dumping the bitmap to a file?
    4. 
     */
    private static final Logger LOGGER = LogManager.getLogger();
    private final int baseAddress;
    private final int displayWidth;
    private final int displayHeight;
    private final Grid grid;

    public BitmapDisplay(final int baseAddress, final int displayWidth, final int displayHeight) {
        this.baseAddress = baseAddress;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.grid = new Grid(displayHeight, displayWidth);
        this.fillGrid();

        this.setTitle("Syscall: DisplayBitmap");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        this.setMinimumSize(new Dimension(displayWidth, displayHeight));
//        this.setPreferredSize(new Dimension(displayWidth, displayHeight));
//        this.setMaximumSize(new Dimension(displayWidth, displayHeight));
        this.add(new GraphicsPanel(new Dimension(displayWidth, displayHeight), this.grid));
        this.setResizable(false);
    }

    public static void show(final int baseAddress, final int displayWidth, final int displayHeight) {
        final BitmapDisplay display = new BitmapDisplay(baseAddress, displayWidth, displayHeight);
        display.pack();
        display.setVisible(true);
    }

    private void fillGrid() {
        var currentOffset = 0;
        for (int row = 0; row < this.displayHeight; row++) {
            for (int col = 0; col < this.displayWidth; col++) {
                final int address = this.baseAddress + currentOffset;
                try {
                    final var word = Memory.getInstance().getWord(address);
                    final var color = new Color(word);
                    this.grid.setColor(row, col, color);
                } catch (final AddressErrorException e) {
                    LOGGER.error("Error updating color for address {} in bitmap display: {}", address, e);
                    return;
                }
                currentOffset += Memory.WORD_LENGTH_BYTES;
            }
        }
    }
}
