package rars.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.riscv.hardware.Memory;
import rars.simulator.Simulator;
import rars.util.BitmapDisplay;
import rars.venus.VenusUI;

import java.util.Timer;
import java.util.TimerTask;

import static java.util.Objects.requireNonNull;

public final class BitmapDisplayManager {

    private final @NotNull VenusUI mainUI;
    private final @NotNull Memory memory;
    private final long windowCreationDelayMillis;
    private @Nullable BitmapDisplay display;
    private @Nullable Timer timer;
    private boolean wasResized;
    private @Nullable DisplayData lastDisplayData;

    public BitmapDisplayManager(
        final @NotNull VenusUI mainUI,
        final @NotNull Memory memory,
        final @NotNull Simulator simulator,
        final long windowCreationDelayMillis
    ) {
        this.mainUI = mainUI;
        this.memory = memory;
        this.windowCreationDelayMillis = windowCreationDelayMillis;
        this.display = null;
        this.timer = null;
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
                case STOP -> onSimulationStop();
            }
        });
    }

    private void onSimulationStop() {
        if (this.wasResized) {
            assert this.timer != null;
            this.timer.cancel();
            this.timer = null;
            this.wasResized = false;
            assert this.lastDisplayData != null;
            this.resize(
                this.lastDisplayData.baseAddress,
                this.lastDisplayData.displayWidth,
                this.lastDisplayData.displayHeight
            );
        }
    }

    public void show(
        final int baseAddress,
        final int width,
        final int height,
        final @NotNull ProgramStatement stmt
    ) throws ExitingException {
        if (width <= 0 || height <= 0) {
            throw new ExitingException(
                stmt,
                "invalid display size. Width: " + width + ", Height: " + height
            );
        }
        if (this.display == null) {
            this.createDisplay(baseAddress, width, height);
        } else if (this.wasResized) {
            this.lastDisplayData = new DisplayData(baseAddress, width, height);
        } else if (
            this.display.displayWidth != width ||
                this.display.displayHeight != height
        ) {
            this.resize(baseAddress, width, height);
            this.wasResized = true;
            if (this.timer == null) {
                this.timer = new Timer(true);
            }
            this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        wasResized = false;
                    }
                },
                this.windowCreationDelayMillis
            );
        } else if (this.display.getBaseAddress() != baseAddress) {
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
        this.createDisplay(baseAddress, width, height);
    }
    
    private void createDisplay(final int baseAddress, final int width, final int height) {
        this.display = new BitmapDisplay(this.memory, this.mainUI, baseAddress, width, height);
    }

    private record DisplayData(
        int baseAddress,
        int displayWidth,
        int displayHeight
    ) {
    }
}
