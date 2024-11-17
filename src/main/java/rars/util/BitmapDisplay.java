package rars.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.Memory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Flow;

public final class BitmapDisplay extends JFrame implements SimpleSubscriber<MemoryAccessNotice> {
    private static final Logger LOGGER = LogManager.getLogger();

    public final int displayWidth;
    public final int displayHeight;
    private final Grid grid;
    private final GraphicsPanel panel;
    public int baseAddress;
    private int upperAddressBound;

    private Flow.Subscription subscription;

    public BitmapDisplay(final int baseAddress, final int displayWidth, final int displayHeight) {
        this.baseAddress = baseAddress;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.upperAddressBound = baseAddress + (displayWidth * displayHeight * Memory.WORD_LENGTH_BYTES);
        this.grid = new Grid(displayHeight, displayWidth);

        this.setTitle("Syscall: DisplayBitmap");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panel = new GraphicsPanel(new Dimension(displayWidth, displayHeight), this.grid);
        this.add(this.panel);
        this.setResizable(false);

        this.fillGrid();
        this.pack();

        try {
            Memory.getInstance().subscribe(this, baseAddress, upperAddressBound);
        } catch (final AddressErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeBaseAddress(final int newBaseAddress) {
        Memory.getInstance().deleteSubscriber(this);
        this.baseAddress = newBaseAddress;
        this.upperAddressBound = newBaseAddress + (this.displayWidth * this.displayHeight * Memory.WORD_LENGTH_BYTES);
        try {
            Memory.getInstance().subscribe(this, this.baseAddress, this.upperAddressBound);
        } catch (final AddressErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillGrid() {
        var currentOffset = 0;
        for (int row = 0; row < this.displayHeight; row++) {
            for (int col = 0; col < this.displayWidth; col++) {
                final int address = this.baseAddress + currentOffset;
                try {
                    final var word = Memory.getInstance().getWordNoNotify(address);
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

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final @NotNull MemoryAccessNotice item) {
        this.doUpdate(item);
        this.panel.repaint();
        this.subscription.request(1);
    }

    private void doUpdate(final @NotNull MemoryAccessNotice notice) {
        if (notice.getAccessType() == AccessNotice.AccessType.WRITE) {
            final int address = notice.getAddress();
            final int length = notice.getLength();
            // figure out which pixels were changed
            final int endAddress = address + length;
            if (endAddress < this.baseAddress || address > this.upperAddressBound) {
                return;
            }
            // clamp the range to the display bounds
            // the memory written may not be aligned by 4 bytes, so we round
            // the start and end to the nearest 4 byte boundary
            final var start = (Math.max(address, this.baseAddress) / 4) * 4;
            final var end = ((Math.min(endAddress, this.upperAddressBound) + 3) / 4) * 4;
            // these values are already nicely aligned, so all that's left to
            // do is to update the grid
            var row = (start - this.baseAddress) / (this.displayWidth * 4);
            var col = (start - this.baseAddress) % (this.displayWidth * 4) / 4;
            for (int i = start; i < end; i += 4) {
                try {
                    final var word = Memory.getInstance().getWordNoNotify(i);
                    final var color = new Color(word);
                    this.grid.setColor(row, col, color);
                } catch (final AddressErrorException e) {
                    LOGGER.error("Error updating color for address {} in bitmap display: {}", i, e);
                    return;
                }
                col++;
                if (col == this.displayWidth) {
                    col = 0;
                    row++;
                }
            }
        }
    }
}
