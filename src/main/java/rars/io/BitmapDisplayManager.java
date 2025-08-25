package rars.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.riscv.hardware.Memory;
import rars.simulator.Simulator;
import rars.util.BitmapDisplay;

import java.util.Timer;
import java.util.TimerTask;

import static java.util.Objects.requireNonNull;

public final class BitmapDisplayManager {
    private final @NotNull Memory memory;
    private final long windowCreationDelayMillis;
    private @Nullable BitmapDisplay display;
    private final @NotNull Timer timer;
    private boolean wasResized;
    private @Nullable DisplayData lastDisplayData;

    public BitmapDisplayManager(
        final @NotNull Memory memory,
        final @NotNull Simulator simulator,
        final long windowCreationDelayMillis
    ) {
        this.memory = memory;
        this.windowCreationDelayMillis = windowCreationDelayMillis;
        this.display = null;
        this.timer = new Timer(true);
        this.wasResized = false;
        simulator.simulatorNoticeHook.subscribe(notice -> {
            switch (notice.action()) {
                case START -> {
                    if (this.display != null) {
                        this.display.unsubscribeFromMemory();
                        this.display.dispose();
                    }
                    this.display = null;
                }
                case STOP -> {
                    if (this.wasResized) {
                        this.timer.cancel();
                        this.wasResized = false;
                        assert this.lastDisplayData != null;
                        this.resize(
                          this.lastDisplayData.baseAddress,
                          this.lastDisplayData.displayWidth,
                          this.lastDisplayData.displayHeight  
                        );
                    }
                }
            }
        });
    }

    public void show(final int baseAddress, final int width, final int height) {
        if (this.display == null) {
            this.display = new BitmapDisplay(this.memory, baseAddress, width, height);
        } else if (this.wasResized) {
            this.lastDisplayData = new DisplayData(baseAddress, width, height);
        } else if (this.display.displayWidth != width || this.display.displayHeight != height) {
            this.resize(baseAddress, width, height);
            this.wasResized = true;
            this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        wasResized = false;
                    }
                },
                this.windowCreationDelayMillis
            );
        } else if (this.display.baseAddress != baseAddress) {
            this.display.changeBaseAddress(baseAddress);
            this.display.repaint();
        }
        if (!this.display.isVisible()) {
            this.display.setVisible(true);
        }
    }

    private void resize(final int baseAddress, final int width, final int height) {
        requireNonNull(this.display).unsubscribeFromMemory();
        this.display.dispose();
        this.display = new BitmapDisplay(this.memory, baseAddress, width, height);
    }

    private record DisplayData(int baseAddress, int displayWidth, int displayHeight) {
    }
}
