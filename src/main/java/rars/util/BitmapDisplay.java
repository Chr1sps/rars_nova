package rars.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

// TODO: move this class to the common SystemIO class

public final class BitmapDisplay extends JFrame {

    private static final @NotNull Logger LOGGER = LogManager.getLogger(BitmapDisplay.class);

    public final int displayWidth;
    public final int displayHeight;
    private final @NotNull Grid grid;
    private final @NotNull GraphicsPanel panel;
    private final @NotNull Consumer<@NotNull MemoryAccessNotice> accessNoticeCallback;
    public int baseAddress;
    private int upperAddressBound;

    public BitmapDisplay(
        final int baseAddress,
        final int displayWidth,
        final int displayHeight
    ) {
        this.baseAddress = baseAddress;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.upperAddressBound = baseAddress + (displayWidth * displayHeight * DataTypes.WORD_SIZE);
        this.grid = new Grid(displayHeight, displayWidth);

        this.setTitle("Syscall: DisplayBitmap");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panel = new GraphicsPanel(new Dimension(displayWidth, displayHeight), this.grid);
        this.add(this.panel);
        this.setResizable(false);

        this.fillGrid();
        this.pack();

        this.accessNoticeCallback = notice -> {
            if (notice.accessType == AccessNotice.AccessType.WRITE) {
                this.updateDisplay(notice.address, notice.length);
            }
        };

        try {
            Globals.MEMORY_INSTANCE.subscribe(this.accessNoticeCallback, baseAddress, upperAddressBound);
        } catch (final AddressErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeBaseAddress(final int newBaseAddress) {
        Globals.MEMORY_INSTANCE.deleteSubscriber(this.accessNoticeCallback);
        this.baseAddress = newBaseAddress;
        this.upperAddressBound = newBaseAddress + (this.displayWidth * this.displayHeight * DataTypes.WORD_SIZE);
        try {
            Globals.MEMORY_INSTANCE.subscribe(
                this.accessNoticeCallback,
                this.baseAddress,
                this.upperAddressBound
            );
        } catch (final AddressErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void unsubscribeFromMemory() {
        Globals.MEMORY_INSTANCE.deleteSubscriber(this.accessNoticeCallback);
    }

    private void fillGrid() {
        var currentOffset = 0;
        for (int row = 0; row < this.displayHeight; row++) {
            for (int col = 0; col < this.displayWidth; col++) {
                final int address = this.baseAddress + currentOffset;
                try {
                    final var word = Globals.MEMORY_INSTANCE.getWordNoNotify(address);
                    final var color = new Color(word);
                    this.grid.setColor(row, col, color);
                } catch (final AddressErrorException e) {
                    LOGGER.error("Error updating color for address {} in bitmap display: {}", address, e);
                    return;
                }
                currentOffset += DataTypes.WORD_SIZE;
            }
        }
    }

    private void updateDisplay(final int memoryAddress, final int writeLength) {

        // figure out which pixels were changed
        final int endAddress = memoryAddress + writeLength;
        // clamp the range to the display bounds
        if (endAddress >= this.baseAddress && memoryAddress <= this.upperAddressBound) {
            // the memory written may not be aligned by 4 bytes, so we round
            // the start and end to the nearest 4 byte boundary
            final var start = (Math.max(memoryAddress, this.baseAddress) / 4) * 4;
            final var end = ((Math.min(endAddress, this.upperAddressBound) + 3) / 4) * 4;
            // these values are already nicely aligned, so all that's left to
            // do is to update the grid
            var row = (start - this.baseAddress) / (this.displayWidth * 4);
            var col = (start - this.baseAddress) % (this.displayWidth * 4) / 4;
            for (int i = start; i < end; i += 4) {
                try {
                    final var word = Globals.MEMORY_INSTANCE.getWordNoNotify(i);
                    final var color = new Color(word);
                    this.grid.setColor(row, col, color);
                } catch (final AddressErrorException e) {
                    LOGGER.error("Error updating color for address {} in bitmap display: {}", i, e);
                    break;
                }
                col++;
                if (col == this.displayWidth) {
                    col = 0;
                    row++;
                }
            }
        }
        this.panel.repaint();
    }
}
