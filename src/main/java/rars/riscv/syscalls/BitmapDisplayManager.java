package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.Memory;
import rars.simulator.Simulator;
import rars.util.BitmapDisplay;

public final class BitmapDisplayManager {
    // public static @NotNull DisplayBitmapImpl INSTANCE = new DisplayBitmapImpl(Globals.MEMORY_INSTANCE);
    private final @NotNull Memory memory;
    private @Nullable BitmapDisplay display;

    public BitmapDisplayManager(
        final @NotNull Memory memory,
        final @NotNull Simulator simulator
    ) {
        this.memory = memory;
        this.display = null;
        simulator.simulatorNoticeHook.subscribe(notice -> {
            if (notice.action() == SimulatorNotice.Action.START) {
                if (this.display != null) {
                    this.display.dispose();
                }
                this.display = null;
            }
        });
    }

    public void show(final int baseAddress, final int width, final int height) {
        if (this.display == null) {
            this.display = new BitmapDisplay(this.memory, baseAddress, width, height);
        } else if (this.display.displayWidth != width || this.display.displayHeight != height) {
            this.display.unsubscribeFromMemory();
            this.display.dispose();
            this.display = new BitmapDisplay(this.memory, baseAddress, width, height);
        } else if (this.display.baseAddress != baseAddress) {
            this.display.changeBaseAddress(baseAddress);
            this.display.repaint();
        }
        if (!this.display.isVisible()) {
            this.display.setVisible(true);
        }
    }
}
